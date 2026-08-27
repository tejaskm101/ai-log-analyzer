Absolutely. Since this is specifically a **README artifact** for your project, I'll make it interview-oriented: clear architecture, class responsibilities, request flow, RAG flow, MCP flow, configuration, and likely interview talking points—without making the project sound more complicated than it is.

# AI-Powered Application Log Analyzer

An AI-powered backend that analyzes application logs using **Google Gemini**, enriches analysis with **Retrieval-Augmented Generation (RAG)** using **PostgreSQL + pgvector**, and exposes selected capabilities through an **MCP (Model Context Protocol) server**.

The project is built with **Java, Spring Boot, Spring AI, PostgreSQL, pgvector, and Maven**.

---

## 1. What Does the Project Do?

The application accepts raw application logs and produces a structured analysis containing:

* **Severity** of the issue
* **Probable cause**
* **Evidence** from the logs
* **Recommended investigation steps**

The important part is that the system does not rely only on Gemini's general knowledge.

For every new log:

```text
New Application Log
        ↓
Save Log in PostgreSQL
        ↓
Generate Embedding
        ↓
Search pgvector for Similar Historical Logs
        ↓
Retrieve Relevant Context
        ↓
Send Current Log + Historical Context to Gemini
        ↓
Generate NEW Analysis
        ↓
Save Analysis in PostgreSQL
        ↓
Return Structured Response
```

This is the core **RAG workflow**.

---

# 2. Technology Stack

| Technology        | Purpose                                       |
| ----------------- | --------------------------------------------- |
| Java 26           | Backend programming language                  |
| Spring Boot 4.1.1 | Backend framework                             |
| Spring AI 2.0.1   | AI and MCP integration                        |
| Google Gemini     | Log analysis and embeddings                   |
| PostgreSQL        | Persistent data storage                       |
| pgvector          | Vector storage and semantic similarity search |
| Maven             | Dependency management and build               |
| Streamable HTTP   | MCP server transport                          |

---

# 3. Project Architecture

The main application follows a layered architecture:

```text
Controller
    ↓
Service Layer
    ↓
Repository Layer
    ↓
PostgreSQL

AIService
    ↓
Gemini

RAGService
    ↓
Embedding Model
    ↓
pgvector

MCPToolService
    ↓
AIService / RAGService / LogRepository
    ↓
MCP Server
```

---

# 4. Package Structure

```text
com.example.AILogAnalyzer
│
├── config
│   └── MCPConfig
│
├── controller
│   └── AIController
│
├── dto
│   └── LogAnalysisResponseDTO
│   └── ...
│
├── entity
│   ├── Log
│   └── LogAnalysis
│
├── repository
│   ├── LogRepository
│   └── LogAnalysisRepository
│
└── service
    ├── AIService
    ├── LogService
    ├── RAGService
    └── MCPToolService
```

---

# 5. Entity Classes

## `Log`

**Location:**

```text
entity/Log.java
```

Represents an application log stored in PostgreSQL.

Its main responsibility is to persist the original/raw log submitted by the user.

Conceptually:

```text
Log
├── id
└── rawContent
```

### Remember

`Log` represents the **input log**.

---

## `LogAnalysis`

**Location:**

```text
entity/LogAnalysis.java
```

Represents the AI-generated analysis associated with a particular log.

It stores the generated analysis and maintains a relationship with the corresponding `Log`.

Conceptually:

```text
Log
  │
  └── LogAnalysis
          └── analysis
```

### Remember

`LogAnalysis` represents the **AI output**.

---

# 6. DTO Classes

## `LogAnalysisResponseDTO`

This DTO represents the structured response returned by Gemini.

It contains:

```text
severity
probableCause
evidence
recommendations
```

For example:

```json
{
  "severity": "High",
  "probableCause": "Database connectivity failure",
  "evidence": "Database connection timeout",
  "recommendations": [
    "Check database health",
    "Inspect connection pool",
    "Check network connectivity"
  ]
}
```

### Why use a DTO?

The DTO prevents the API response from being tightly coupled to the database entity.

The database model and API response can therefore evolve independently.

---

# 7. Repository Classes

## `LogRepository`

```text
repository/LogRepository.java
```

Extends:

```java
JpaRepository<Log, Long>
```

Provides database operations for `Log`.

Because it extends `JpaRepository`, common operations such as:

```text
save()
findById()
findAll()
delete()
```

are available without manually implementing SQL queries.

---

## `LogAnalysisRepository`

Works similarly for `LogAnalysis`.

It is responsible for persisting the AI-generated analysis.

---

# 8. Service Classes

## `LogService`

Responsible for application-log persistence.

Its primary responsibility is saving incoming raw logs into PostgreSQL.

Conceptually:

```text
Raw Log
   ↓
LogService
   ↓
LogRepository
   ↓
PostgreSQL
```

It keeps database-related log persistence separate from the controller.

---

# 9. `AIService`

This is the main **Gemini integration layer**.

```text
service/AIService.java
```

It uses Spring AI's `ChatClient` to communicate with Gemini.

### Main responsibility

It receives:

```text
Current Log
+
Relevant Historical Logs
```

and constructs a prompt for Gemini.

The prompt asks Gemini to determine:

1. Severity
2. Probable cause
3. Evidence
4. Recommended investigation steps

Gemini then returns the result as:

```text
LogAnalysisResponseDTO
```

### Important RAG concept

The AIService does **not** simply ask Gemini:

```text
"Analyze this log."
```

Instead, it provides:

```text
Current Log
+
Retrieved Historical Context
```

Therefore, Gemini can use previous similar incidents as additional context.

### Persistence

After Gemini produces the response, `AIService` converts the structured response into an analysis string and stores it through:

```text
LogAnalysisRepository
```

---

# 10. `RAGService`

This is the core **Retrieval-Augmented Generation** component.

```text
service/RAGService.java
```

It uses Spring AI's:

```text
VectorStore
```

which is backed by **PostgreSQL + pgvector**.

It has two important responsibilities.

---

## Storing Logs

```text
storeLog(String content)
```

A log is converted into a Spring AI `Document` and added to the vector store.

The configured embedding model converts the text into a numerical vector.

Conceptually:

```text
Application Log
      ↓
Embedding Model
      ↓
Vector
      ↓
pgvector
```

The vector represents the semantic meaning of the log.

---

## Retrieving Similar Logs

```text
retrieveSimilarLogs(String content)
```

The incoming log is used as a similarity-search query.

The system requests the:

```text
Top 3
```

most semantically similar historical logs.

Conceptually:

```text
New Log
   ↓
Embedding
   ↓
Vector Similarity Search
   ↓
pgvector
   ↓
Top 3 Similar Historical Logs
```

### Important

The retrieved logs are **not returned directly as the final answer**.

They are passed to Gemini as context.

Therefore:

```text
Historical Logs
       ↓
   Retrieved
       ↓
Given to Gemini
       ↓
Gemini generates NEW analysis
```

This is the key distinction between **RAG** and simply retrieving old answers.

---

# 11. `AIController`

```text
controller/AIController.java
```

This is the REST API entry point.

It exposes:

```text
POST /api/ai/analyze
```

The controller receives the raw application log.

The main flow is:

```text
HTTP Request
      ↓
AIController
      ↓
LogService
      ↓
RAGService
      ↓
AIService
      ↓
Response
```

The controller coordinates the request but does not contain the actual AI or database logic.

---

# 12. Main REST Request Flow

When a user sends:

```text
POST /api/ai/analyze
```

with a raw log:

```text
2026-08-27 ERROR PaymentService
Database connection timeout
```

the application performs:

### Step 1 — Save the log

```text
AIController
    ↓
LogService
    ↓
PostgreSQL
```

### Step 2 — Retrieve historical context

```text
RAGService
    ↓
pgvector
    ↓
Top 3 similar logs
```

### Step 3 — Generate analysis

```text
Current Log
+
Historical Logs
      ↓
   AIService
      ↓
    Gemini
```

### Step 4 — Save the analysis

```text
AIService
    ↓
LogAnalysisRepository
    ↓
PostgreSQL
```

### Step 5 — Return response

```text
LogAnalysisResponseDTO
        ↓
    AIController
        ↓
      Client
```

---

# 13. MCP Integration

The project also exposes selected backend capabilities through an **MCP server**.

MCP allows an MCP-compatible AI client to discover and invoke tools exposed by our application.

Our MCP endpoint is:

```text
/mcp
```

using:

```text
Streamable HTTP
```

---

# 14. `MCPToolService`

```text
service/MCPToolService.java
```

This class exposes our application functionality as MCP tools using Spring AI's `@Tool` annotation.

We currently expose three tools.

---

## Tool 1 — `analyzeLog`

```text
Analyze an application log using AI and historical context
```

This tool performs the complete analysis pipeline:

```text
Raw Log
   ↓
LogService
   ↓
RAGService
   ↓
Similar Historical Logs
   ↓
AIService
   ↓
Gemini
   ↓
Analysis
```

It essentially makes the application's main AI-analysis capability available through MCP.

---

## Tool 2 — `searchSimilarLogs`

```text
Search for the three most semantically similar historical application logs
```

This tool directly exposes the RAG retrieval capability.

```text
Log
 ↓
RAGService
 ↓
pgvector
 ↓
Top 3 Similar Logs
```

Unlike `analyzeLog`, it does **not** ask Gemini to generate an analysis.

It simply returns the relevant historical logs.

---

## Tool 3 — `getLogHistory`

```text
Retrieve all previously stored application logs
```

This tool uses:

```text
LogRepository.findAll()
```

to retrieve previously stored logs from PostgreSQL.

---

# 15. `MCPConfig`

```text
config/MCPConfig.java
```

This class registers the methods annotated with `@Tool` as Spring AI tool callbacks.

It uses:

```text
MethodToolCallbackProvider
```

to convert the methods in `MCPToolService` into MCP-compatible tools.

Conceptually:

```text
MCPToolService
      ↓
@Tool methods
      ↓
MethodToolCallbackProvider
      ↓
ToolCallbacks
      ↓
MCP Server
```

This is what makes the three methods discoverable as MCP tools.

---

# 16. MCP Configuration

The MCP server is configured using:

```properties
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.name=ai-log-analyzer
spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp
```

Therefore:

```text
MCP Client
     ↓
HTTP
     ↓
localhost:8080/mcp
     ↓
Spring AI MCP Server
```

---

# 17. RAG vs MCP — Important Interview Question

These are **different concepts**.

### RAG

RAG improves the AI's response by retrieving relevant information before generation.

```text
Current Log
    ↓
Retrieve Similar Logs
    ↓
Historical Context
    ↓
Gemini
    ↓
New Analysis
```

### MCP

MCP provides a standardized way for an external AI client to interact with application capabilities.

```text
AI Client
    ↓
MCP
    ↓
Our Tools
    ↓
Application Services
```

### In our project

RAG answers:

> "What relevant historical information should Gemini use?"

MCP answers:

> "How can an external AI client interact with our application's log-analysis capabilities?"

---

# 18. Why pgvector?

Traditional databases are good at exact-value searches.

For example:

```text
WHERE service = 'PaymentService'
```

But application logs can express the same problem using different wording.

For example:

```text
Database connection timeout
```

and:

```text
Unable to establish database connection
```

may describe essentially the same problem.

Vector embeddings represent semantic meaning numerically, allowing pgvector to perform **semantic similarity search**.

---

# 19. Why RAG Improves the Application

Without RAG:

```text
Current Log
    ↓
Gemini
    ↓
General-knowledge-based analysis
```

With RAG:

```text
Current Log
    ↓
Find Similar Historical Logs
    ↓
Current Log + Historical Context
    ↓
Gemini
    ↓
Context-aware analysis
```

This allows the model to use knowledge specific to previously observed incidents in the application's environment.

---

# 20. Important Design Decision

The system stores the **raw log in the vector store**, not the previous AI response.

Therefore, when similar logs are retrieved, Gemini receives:

```text
Historical Log 1
Historical Log 2
Historical Log 3
```

rather than:

```text
Previous Gemini Response 1
Previous Gemini Response 2
Previous Gemini Response 3
```

Gemini then generates a **new response for the current log**.

This prevents the RAG system from simply copying previous answers.

---

# 21. Complete Architecture

```text
                    ┌─────────────────────┐
                    │      Client         │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    AIController     │
                    │ POST /api/ai/analyze│
                    └──────────┬──────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
        ┌──────────────┐              ┌──────────────┐
        │  LogService  │              │  RAGService  │
        └──────┬───────┘              └──────┬───────┘
               │                             │
               ▼                             ▼
        ┌──────────────┐              ┌──────────────┐
        │  PostgreSQL  │              │   pgvector   │
        │    Logs      │              │ Similar Logs │
        └──────────────┘              └──────┬───────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │    AIService    │
                                    │   Spring AI     │
                                    └────────┬────────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │  Google Gemini  │
                                    └────────┬────────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │ LogAnalysisDTO  │
                                    └────────┬────────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │   PostgreSQL    │
                                    │    Analysis     │
                                    └─────────────────┘


                         MCP Extension
                              │
                              ▼
                       ┌──────────────┐
                       │ MCP Client   │
                       └──────┬───────┘
                              │
                       Streamable HTTP
                              │
                              ▼
                       ┌──────────────┐
                       │ MCP Server   │
                       │    /mcp      │
                       └──────┬───────┘
                              │
                              ▼
                       ┌──────────────┐
                       │MCPToolService│
                       └──────┬───────┘
                              │
             ┌────────────────┼────────────────┐
             ▼                ▼                ▼
        analyzeLog    searchSimilarLogs   getLogHistory
             │                │                │
             ▼                ▼                ▼
        AIService        RAGService      LogRepository
```

---

# 22. Interview Quick Revision

## If asked: "What is the main purpose of the project?"

> It is a Spring Boot backend that analyzes application logs using Gemini and improves the analysis using RAG. Historical logs are embedded and stored in pgvector, semantically similar logs are retrieved, and those logs are provided to Gemini as context to generate a new structured analysis. I also exposed the core capabilities through an MCP server.

---

## If asked: "Explain your RAG implementation."

> I store application logs as vectorized documents in PostgreSQL using pgvector. When a new log arrives, I perform a semantic similarity search and retrieve the top three relevant historical logs. These logs are then passed as additional context to Gemini along with the current log, allowing Gemini to generate a new, context-aware analysis.

---

## If asked: "Does RAG return previous answers?"

> No. The vector store contains historical logs rather than previous AI responses. The retrieved logs are supplied to Gemini as context, and Gemini generates a new analysis specifically for the current log.

---

## If asked: "Why use embeddings?"

> Embeddings allow the system to represent the semantic meaning of logs as vectors. This means we can find logs that are conceptually similar even when their exact wording is different.

---

## If asked: "Why pgvector?"

> It allows us to keep vector data alongside our existing PostgreSQL data and perform similarity searches without introducing a separate vector database.

---

## If asked: "What does AIService do?"

> AIService is responsible for communicating with Gemini through Spring AI. It constructs the prompt containing the current log and retrieved historical context, converts Gemini's structured response into our DTO, and persists the resulting analysis.

---

## If asked: "What does RAGService do?"

> RAGService manages the vector-store operations. It stores logs as documents and retrieves the top three semantically similar historical logs using pgvector.

---

## If asked: "What does MCP add to the project?"

> MCP exposes selected application capabilities as standardized tools that an MCP-compatible AI client can discover and invoke. In our project, we expose log analysis, semantic log search, and historical log retrieval.

---

## If asked: "What are your MCP tools?"

```text
analyzeLog
searchSimilarLogs
getLogHistory
```

---

## If asked: "Why MCP instead of simply exposing REST APIs?"

> REST APIs are designed for conventional application-to-application communication. MCP provides a standardized tool interface specifically designed for AI clients, allowing AI systems to discover available capabilities and invoke them using a common protocol.

---

# 23. One-Minute Project Explanation

> I built an AI-powered application log analyzer using Java and Spring Boot. The backend integrates the Gemini API through Spring AI to analyze application logs and generate structured information such as severity, probable cause, evidence, and recommended investigation steps.
>
> I implemented RAG using PostgreSQL and pgvector. Logs are embedded and stored as vectors, and when a new log arrives, the system retrieves the three most semantically similar historical logs. Those logs are provided to Gemini as contextual information, allowing it to generate a new analysis rather than simply returning previous responses.
>
> Finally, I added an MCP server using Spring AI's Streamable HTTP support. It exposes three tools — log analysis, semantic log search, and historical log retrieval — allowing MCP-compatible AI clients to interact with the backend's capabilities.

---

# 24. Current MCP Testing Note

The MCP server successfully:

* Starts on the configured endpoint
* Registers all three MCP tools
* Establishes an MCP handshake with an MCP client

During development, client-side tool discovery/invocation encountered compatibility/API issues with the clients tested.

The important distinction is:

```text
MCP server implementation       ✅
Tool registration               ✅
MCP handshake                   ✅
End-to-end tool invocation      ⚠️ Not fully verified
```

Do not claim that end-to-end MCP tool invocation was successfully demonstrated unless it is verified later.

---

# 25. Key Classes to Remember

For interview revision, focus primarily on these classes:

```text
AIController
     ↓
LogService
     ↓
RAGService
     ↓
AIService
     ↓
MCPToolService
     ↓
MCPConfig
```

### One-line responsibilities

| Class                    | Responsibility                                         |
| ------------------------ | ------------------------------------------------------ |
| `AIController`           | Receives the REST request                              |
| `LogService`             | Saves application logs                                 |
| `RAGService`             | Stores and retrieves vectorized logs                   |
| `AIService`              | Sends contextual prompts to Gemini and stores analysis |
| `MCPToolService`         | Exposes application capabilities as MCP tools          |
| `MCPConfig`              | Registers `@Tool` methods as tool callbacks            |
| `LogRepository`          | Database operations for logs                           |
| `LogAnalysisRepository`  | Database operations for analyses                       |
| `Log`                    | Represents stored application log                      |
| `LogAnalysis`            | Represents AI-generated analysis                       |
| `LogAnalysisResponseDTO` | Represents structured API/AI response                  |

---

## Final Mental Model

If you remember only one thing before the interview, remember this:

```text
                CURRENT LOG
                     │
                     ▼
               Save to DB
                     │
                     ▼
              Semantic Search
                     │
                     ▼
              Top 3 Old Logs
                     │
                     ▼
        CURRENT LOG + OLD LOGS
                     │
                     ▼
                  GEMINI
                     │
                     ▼
              NEW ANALYSIS
                     │
                     ▼
               Save Analysis
                     │
                     ▼
                 Response


        MCP exposes this functionality
        to AI clients as discoverable tools.
```

**Core idea:**

> **RAG gives Gemini relevant historical context; MCP gives external AI clients access to our application's capabilities.**
