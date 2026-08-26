package com.example.AILogAnalyzer.service;

import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.entity.LogAnalysis;
import com.example.AILogAnalyzer.repository.LogAnalysisRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final LogAnalysisRepository logAnalysisRepository;

    public AIService(ChatClient.Builder chatClientBuilder,
                     LogAnalysisRepository logAnalysisRepository) {
        this.chatClient = chatClientBuilder.build();
        this.logAnalysisRepository = logAnalysisRepository;
    }

    public String analyzeLog(Log log) {

        String analysis = chatClient.prompt()
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
                .content();

        LogAnalysis logAnalysis = new LogAnalysis();
        logAnalysis.setLog(log);
        logAnalysis.setAnalysis(analysis);

        logAnalysisRepository.save(logAnalysis);

        return analysis;
    }
}