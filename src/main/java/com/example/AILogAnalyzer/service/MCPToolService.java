package com.example.AILogAnalyzer.service;

import com.example.AILogAnalyzer.dto.LogAnalysisResponseDTO;
import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.repository.LogRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MCPToolService {

    private final AIService aiService;
    private final RAGService ragService;
    private final LogService logService;
    private final LogRepository logRepository;

    public MCPToolService(AIService aiService,
                          RAGService ragService,
                          LogService logService,
                          LogRepository logRepository) {
        this.aiService = aiService;
        this.ragService = ragService;
        this.logService = logService;
        this.logRepository = logRepository;
    }

    @Tool(description = "Analyze an application log using AI and historical context")
    public LogAnalysisResponseDTO analyzeLog(String rawLog) {

        Log log = logService.saveLog(rawLog);

        List<Document> similarLogs =
                ragService.retrieveSimilarLogs(log.getRawContent());

        LogAnalysisResponseDTO analysis =
                aiService.analyzeLog(log, similarLogs);

        ragService.storeLog(log.getRawContent());

        return analysis;
    }

    @Tool(description = "Search for the three most semantically similar historical application logs")
    public List<String> searchSimilarLogs(String log) {

        return ragService.retrieveSimilarLogs(log)
                .stream()
                .map(Document::getText)
                .toList();
    }

    @Tool(description = "Retrieve all previously stored application logs")
    public List<Log> getLogHistory() {

        return logRepository.findAll();
    }
}
