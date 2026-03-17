package com.flakespy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flakespy.model.TestResult;
import com.flakespy.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAIService {

    private final WebClient.Builder webClientBuilder;
    private final TestResultRepository testResultRepository;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.model}")
    private String groqModel;

    // analyse a flaky test and save root cause + fix suggestion
    public void analyseFlakyTest(String testName, Long projectId) {
        // get all FAILED results for this test
        List<TestResult> failures = testResultRepository
                .findByTestNameAndStatus(testName, "FAILED");

        if (failures.isEmpty()) {
            log.info("No failures found for test: {}", testName);
            return;
        }

        // build a summary of failure messages to send to Groq
        StringBuilder failureSummary = new StringBuilder();
        int limit = Math.min(failures.size(), 5); // send max 5 examples

        for (int i = 0; i < limit; i++) {
            TestResult f = failures.get(i);
            failureSummary
                .append("Failure ").append(i + 1).append(":\n")
                .append("Message: ").append(f.getFailureMessage()).append("\n")
                .append("Stack trace (first 300 chars): ")
                .append(truncate(f.getStackTrace(), 300))
                .append("\n\n");
        }

        String prompt = buildPrompt(testName, failureSummary.toString(),
                failures.size());

        String aiResponse = callGroq(prompt);

        if (aiResponse == null) {
            log.warn("No AI response for test: {}", testName);
            return;
        }

        saveAiAnalysis(testName, projectId, aiResponse);
    }

    // builds the structured prompt sent to Groq
    private String buildPrompt(String testName, String failureSummary,
            int totalFailures) {
        return """
            You are a QA automation expert specialising in flaky test analysis.

            Analyse the following flaky test and respond ONLY with a valid JSON
            object. No explanation, no markdown, no extra text — just the JSON.

            Test name: %s
            Total failures recorded: %d

            Failure examples:
            %s

            Respond with this exact JSON structure:
            {
              "rootCause": "one of: TIMING | LOCATOR | DATA_DEPENDENCY | NETWORK | CONCURRENCY | ENVIRONMENT | UNKNOWN",
              "rootCauseExplanation": "one sentence explaining why this root cause fits",
              "fixSuggestion": "one specific, actionable fix for this test",
              "failurePattern": "one sentence describing the pattern you see in the failures"
            }
            """.formatted(testName, totalFailures, failureSummary);
    }

    // calls Groq API and returns the raw response text
    private String callGroq(String prompt) {
        try {
            WebClient client = webClientBuilder.build();

            Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,   // low temperature = more consistent output
                "max_tokens", 300
            );

            Map response = client.post()
                    .uri(groqApiUrl)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            // extract the text from choices[0].message.content
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");

            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            return message.get("content").toString();

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            return null;
        }
    }

    // parses AI JSON response and saves to all matching TestResult records
    private void saveAiAnalysis(String testName, Long projectId,
            String aiResponse) {
        try {
            // strip any accidental markdown fences just in case
            String clean = aiResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode json = objectMapper.readTree(clean);

            String rootCause     = json.path("rootCause").asText("UNKNOWN");
            String explanation   = json.path("rootCauseExplanation").asText();
            String fixSuggestion = json.path("fixSuggestion").asText();
            String pattern       = json.path("failurePattern").asText();

            // update all TestResult records for this test with the AI analysis
            List<TestResult> allResults = testResultRepository
                    .findByTestNameAndStatus(testName, "FAILED");

            for (TestResult result : allResults) {
                result.setRootCause(rootCause);
                result.setFixSuggestion(fixSuggestion);
            }

            testResultRepository.saveAll(allResults);

            log.info("AI analysis saved for test: {} → rootCause: {}",
                    testName, rootCause);

        } catch (Exception e) {
            log.error("Failed to parse AI response for {}: {}",
                    testName, e.getMessage());
        }
    }

    // analyse all flaky tests for a project (called by NightlyScheduler)
    public void analyseAllFlakyTests(Long projectId,
            List<String> flakyTestNames) {
        log.info("Starting AI analysis for {} flaky tests", flakyTestNames.size());

        for (String testName : flakyTestNames) {
            try {
                analyseFlakyTest(testName, projectId);

                // small delay between calls — respect Groq rate limits
                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("AI analysis interrupted");
                break;
            } catch (Exception e) {
                log.error("AI analysis failed for {}: {}", testName,
                        e.getMessage());
            }
        }

        log.info("AI analysis complete for project {}", projectId);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
