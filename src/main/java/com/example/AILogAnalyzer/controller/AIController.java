package com.example.AILogAnalyzer.controller;

import com.example.AILogAnalyzer.dto.LogAnalysisResponseDTO;
import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.service.AIService;
import com.example.AILogAnalyzer.service.LogService;
import com.example.AILogAnalyzer.service.RAGService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;
    private final LogService logService;
    private final RAGService ragService;

    public AIController(AIService aiService,
                        LogService logService,
                        RAGService ragService) {
        this.aiService = aiService;
        this.logService = logService;
        this.ragService = ragService;
    }

    @PostMapping("/analyze")
    public LogAnalysisResponseDTO analyzeLog(@RequestBody String rawLog) {

        Log log = logService.saveLog(rawLog);

        List<Document> similarLogs =
                ragService.retrieveSimilarLogs(log.getRawContent());

        LogAnalysisResponseDTO analysis =
                aiService.analyzeLog(log, similarLogs);

        ragService.storeLog(log.getRawContent());

        return analysis;
    }
}