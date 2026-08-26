package com.example.AILogAnalyzer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "log_analysis")
public class LogAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    public Long getId() {
        return id;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
}
