package com.flakespy.service;

import com.flakespy.model.GitLabProject;
import com.flakespy.model.TestResult;
import com.flakespy.model.TestRun;
import com.flakespy.repository.TestResultRepository;
import com.flakespy.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class XmlParserService {

    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;

    public void parseAndSave(String xml, GitLabProject project, Map<String, Object> jobMeta) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            doc.getDocumentElement().normalize();

            // build the TestRun from job metadata
            TestRun run = new TestRun();
            run.setGitLabProject(project);
            run.setGitlabJobId(Long.valueOf(jobMeta.get("id").toString()));
            run.setGitlabJobName(jobMeta.get("name").toString());
            run.setBranch(extractBranch(jobMeta));
            run.setRunDate(parseRunDate(jobMeta));

            // read totals from <testsuite> element
            Element suite = doc.getDocumentElement();
            run.setTotalTests(intAttr(suite, "tests"));
            run.setFailed(intAttr(suite, "failures") + intAttr(suite, "errors"));
            run.setSkipped(intAttr(suite, "skipped"));
            run.setPassed(run.getTotalTests() - run.getFailed() - run.getSkipped());

            TestRun savedRun = testRunRepository.save(run);

            // parse individual <testcase> elements
            NodeList testCases = doc.getElementsByTagName("testcase");
            List<TestResult> results = new ArrayList<>();

            for (int i = 0; i < testCases.getLength(); i++) {
                Element tc = (Element) testCases.item(i);
                TestResult result = new TestResult();
                result.setTestRun(savedRun);
                result.setTestName(tc.getAttribute("name"));
                result.setClassName(tc.getAttribute("classname"));
                result.setDuration(doubleAttr(tc, "time"));

                // determine status
                NodeList failures = tc.getElementsByTagName("failure");
                NodeList errors   = tc.getElementsByTagName("error");
                NodeList skipped  = tc.getElementsByTagName("skipped");

                if (failures.getLength() > 0) {
                    result.setStatus("FAILED");
                    Element f = (Element) failures.item(0);
                    result.setFailureMessage(f.getAttribute("message"));
                    result.setStackTrace(f.getTextContent());
                } else if (errors.getLength() > 0) {
                    result.setStatus("FAILED");
                    Element e = (Element) errors.item(0);
                    result.setFailureMessage(e.getAttribute("message"));
                    result.setStackTrace(e.getTextContent());
                } else if (skipped.getLength() > 0) {
                    result.setStatus("SKIPPED");
                } else {
                    result.setStatus("PASSED");
                }

                results.add(result);
            }

            testResultRepository.saveAll(results);
            log.info("Saved {} test results for job {}",
                results.size(), savedRun.getGitlabJobId());

        } catch (Exception e) {
            log.error("Failed to parse XML: {}", e.getMessage());
        }
    }

    private String extractBranch(Map<String, Object> jobMeta) {
        try {
            Map<String, Object> ref = (Map<String, Object>) jobMeta.get("ref");
            return ref != null ? ref.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private LocalDateTime parseRunDate(Map<String, Object> jobMeta) {
        try {
            String raw = jobMeta.get("created_at").toString();
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private int intAttr(Element el, String attr) {
        try { return Integer.parseInt(el.getAttribute(attr)); }
        catch (Exception e) { return 0; }
    }

    private double doubleAttr(Element el, String attr) {
        try { return Double.parseDouble(el.getAttribute(attr)); }
        catch (Exception e) { return 0.0; }
    }
}
