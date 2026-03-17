package com.flakespy.service;

import com.flakespy.model.GitLabProject;
import com.flakespy.model.TestRun;
import com.flakespy.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitLabService {

    private final WebClient.Builder webClientBuilder;
    private final TestRunRepository testRunRepository;
    private final XmlParserService xmlParserService;

    // fetches all successful nightly jobs for a project
    public List<Map<String, Object>> fetchNightlyJobs(GitLabProject project) {
        String url = project.getGitlabUrl()
                + "/api/v4/projects/" + project.getProjectId()
                + "/jobs?scope=success&per_page=30";

        WebClient client = webClientBuilder.build();

        List<Map<String, Object>> jobs = client.get()
                .uri(url)
                .header("PRIVATE-TOKEN", project.getPersonalAccessToken())
                .retrieve()
                .bodyToFlux(Map.class)
                .cast(Map.class)
                .map(m -> (Map<String, Object>) m)
                .collectList()
                .block();

        if (jobs == null) return Collections.emptyList();

        // filter to only the job name QA engineer configured
        return jobs.stream()
                .filter(job -> project.getJobName().equals(job.get("name")))
                .toList();
    }

    // downloads artifact ZIP for a job and extracts XML files
    public List<String> downloadAndExtractXml(GitLabProject project, Long jobId) {
        String url = project.getGitlabUrl()
                + "/api/v4/projects/" + project.getProjectId()
                + "/jobs/" + jobId + "/artifacts";

        WebClient client = webClientBuilder
                .codecs(config -> config
                    .defaultCodecs()
                    .maxInMemorySize(50 * 1024 * 1024)) // 50MB max
                .build();

        byte[] zipBytes = client.get()
                .uri(url)
                .header("PRIVATE-TOKEN", project.getPersonalAccessToken())
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        if (zipBytes == null) {
            log.warn("No artifact found for job {}", jobId);
            return Collections.emptyList();
        }

        return extractXmlFromZip(zipBytes);
    }

    // unzips the artifact and returns all XML file contents as strings
    private List<String> extractXmlFromZip(byte[] zipBytes) {
        List<String> xmlContents = new ArrayList<>();

        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(
                new ByteArrayInputStream(zipBytes))) {

            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {

                // only process XML files (JUnit reports)
                if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[1024];
                    int len;
                    while ((len = zis.read(chunk)) != -1) {
                        buffer.write(chunk, 0, len);
                    }
                    xmlContents.add(buffer.toString());
                    log.info("Extracted XML: {}", entry.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract ZIP: {}", e.getMessage());
        }

        return xmlContents;
    }

    // full sync for one project — called by NightlyScheduler
    public void syncProject(GitLabProject project) {
        log.info("Syncing project: {}", project.getProjectName());

        List<Map<String, Object>> jobs = fetchNightlyJobs(project);

        for (Map<String, Object> job : jobs) {
            Long jobId = Long.valueOf(job.get("id").toString());

            // skip if we already have this run
            if (testRunRepository.existsByGitlabJobId(jobId)) {
                log.info("Job {} already synced, skipping", jobId);
                continue;
            }

            List<String> xmlFiles = downloadAndExtractXml(project, jobId);

            for (String xml : xmlFiles) {
                xmlParserService.parseAndSave(xml, project, job);
            }
        }

        log.info("Sync complete for: {}", project.getProjectName());
    }
}
