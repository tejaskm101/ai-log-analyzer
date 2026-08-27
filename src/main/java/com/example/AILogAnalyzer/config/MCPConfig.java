package com.example.AILogAnalyzer.config;

import com.example.AILogAnalyzer.service.MCPToolService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCPConfig {

    @Bean
    public ToolCallback[] mcpTools(MCPToolService mcpToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(mcpToolService)
                .build()
                .getToolCallbacks();
    }
}