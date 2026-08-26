package com.example.AILogAnalyzer.repository;

import com.example.AILogAnalyzer.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<Log, Long> {
}
