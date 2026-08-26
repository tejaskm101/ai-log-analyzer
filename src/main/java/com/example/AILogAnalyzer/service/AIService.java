package com.example.AILogAnalyzer.service;

import com.example.AILogAnalyzer.dto.LogAnalysisResponseDTO;
import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.entity.LogAnalysis;
import com.example.AILogAnalyzer.repository.LogAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final LogAnalysisRepository logAnalysisRepository;
    private final ObjectMapper objectMapper;

    public AIService(ChatClient.Builder chatClientBuilder,
                     LogAnalysisRepository logAnalysisRepository,
                     ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.logAnalysisRepository = logAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    public LogAnalysisResponseDTO analyzeLog(Log log) {

        LogAnalysisResponseDTO analysis = chatClient.prompt()
                .user("""
                        Analyze the following application logs.

                        Identify:
                        1. The severity of the issue
                        2. The probable cause
                        3. Relevant evidence from the logs
                        4. Recommended investigation steps

                        Logs:
                        %s
                        """.formatted(log.getRawContent()))
                .call()
                .entity(LogAnalysisResponseDTO.class);

        LogAnalysis logAnalysis = new LogAnalysis();
        logAnalysis.setLog(log);

        try {
            logAnalysis.setAnalysis(
                    objectMapper.writeValueAsString(analysis)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize AI analysis", e);
        }

        logAnalysisRepository.save(logAnalysis);

        return analysis;
    }
}