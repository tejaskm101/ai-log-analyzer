package com.example.AILogAnalyzer.repository;

import com.example.AILogAnalyzer.entity.LogAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {
}
