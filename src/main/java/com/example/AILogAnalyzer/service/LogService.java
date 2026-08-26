package com.example.AILogAnalyzer.service;

import com.example.AILogAnalyzer.entity.Log;
import com.example.AILogAnalyzer.repository.LogRepository;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public Log saveLog(String rawContent) {
        Log log = new Log(rawContent);
        return logRepository.save(log);
    }
}
