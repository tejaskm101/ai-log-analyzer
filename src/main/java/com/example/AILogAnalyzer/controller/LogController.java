package com.example.AILogAnalyzer.controller;

import com.example.AILogAnalyzer.dto.LogRequestDTO;
import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.service.LogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Log createLog(@RequestBody LogRequestDTO request) {
        return logService.saveLog(request.getRawContent());
    }
}
