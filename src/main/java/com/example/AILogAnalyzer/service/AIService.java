package com.example.AILogAnalyzer.service;

import com.example.AILogAnalyzer.dto.LogAnalysisResponseDTO;
import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.entity.LogAnalysis;
import com.example.AILogAnalyzer.repository.LogAnalysisRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final LogAnalysisRepository logAnalysisRepository;

    public AIService(ChatClient.Builder chatClientBuilder,
                     LogAnalysisRepository logAnalysisRepository) {
        this.chatClient = chatClientBuilder.build();
        this.logAnalysisRepository = logAnalysisRepository;
    }

    public LogAnalysisResponseDTO analyzeLog(Log log, List<Document> similarLogs) {

        String historicalContext = similarLogs.stream()
                .map(Document::getText)
                .reduce("", (context, oldLog) ->
                        context + "\n--- Historical Log ---\n" + oldLog);

        LogAnalysisResponseDTO analysis = chatClient.prompt()
                .user("""
                        Analyze the following application log.

                        Identify:
                        1. The severity of the issue
                        2. The probable cause
                        3. Relevant evidence from the logs
                        4. Recommended investigation steps

                        Current Log:
                        %s

                        Relevant Historical Logs:
                        %s

                        Use the historical logs as additional context.
                        Generate a NEW analysis specifically for the current log.
                        Do not simply copy or repeat previous information.
                        """.formatted(
                        log.getRawContent(),
                        historicalContext))
                .call()
                .entity(LogAnalysisResponseDTO.class);

        LogAnalysis logAnalysis = new LogAnalysis();
        logAnalysis.setLog(log);

        String analysisText =
                "Severity: " + analysis.getSeverity()
                        + "\nProbable Cause: " + analysis.getProbableCause()
                        + "\nEvidence: " + analysis.getEvidence()
                        + "\nRecommendations: " + analysis.getRecommendations();

        logAnalysis.setAnalysis(analysisText);

        logAnalysisRepository.save(logAnalysis);

        return analysis;
    }
}