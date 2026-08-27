package com.example.AILogAnalyzer.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RAGService {

    private final VectorStore vectorStore;

    public RAGService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeLog(String content) {
        Document document = new Document(content);
        vectorStore.add(List.of(document));
    }

    public List<Document> retrieveSimilarLogs(String content) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(content)
                .topK(3)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }
}


