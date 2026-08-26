package com.example.AILogAnalyzer.controller;

import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.service.AIService;
import com.example.AILogAnalyzer.service.LogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;
    private final LogService logService;

    public AIController(AIService aiService, LogService logService) {
        this.aiService = aiService;
        this.logService = logService;
    }

    @PostMapping("/analyze")
    public String analyzeLog(@RequestBody String rawLog) {

        Log log = logService.saveLog(rawLog);

        return aiService.analyzeLog(log);
    }
}
