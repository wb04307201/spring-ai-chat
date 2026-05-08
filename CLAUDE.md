# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Spring AI LoomAgent (灵梭)** — A Spring Boot auto-configuration library that quickly adds a chat interface to Spring AI applications. Supports RAG (knowledge base), MCP (Model Context Protocol tools), and a Skill library system.

- **Group/Artifact**: `com.gitee.wb04307201` / `spring-ai-loom-agent-parent`
- **Tech Stack**: Java 17+, Spring Boot 3.5.14, Spring AI 1.1.5, Lombok, Flyway, H2
- **Build**: Maven multi-module project

## Module Structure

| Module | Purpose |
|--------|---------|
| `spring-ai-loom-agent` | Core library: interfaces (`IChat`, `IMcp`, `IUser`, `IKnowledge`, `ISkillStorage`, `IFile`, `IEmbedTool`) and default implementations |
| `spring-ai-loom-agent-spring-boot-autoconfigure` | Spring Boot auto-configuration (`LoomAgentConfiguration`) — wires all beans together, defines REST endpoints under `/spring/ai/loom/*` |
| `spring-ai-loom-agent-spring-boot-starter` | Starter POM — single dependency to pull in the autoconfigure module |
| `spring-ai-loom-agent-test` | Demo/test application — runs on port 8089, uses DashScope (qwen3.6-plus) + Qdrant vector store |

## Key Architecture

### Core Components (all in `cn.wubo.spring.ai.loom.agent` package)

- **chat/** — `IChat` interface + `DefaultChat`: orchestrates ChatClient, RAG advisor, MCP tools, skills, and conversation memory. Streaming via SSE at `/spring/ai/loom/stream`.
- **mcp/** — `IMcp` interface with `SyncMcp` (default) and `ASyncMcp` implementations for MCP protocol tool callbacks.
- **knowledge/** — `IKnowledge` + `DefaultKnowledge`: manages knowledge bases backed by vector stores.
- **skill/** — `ISkillStorage` + `DefaultSkillStorage`: skill library for predefined prompt templates with parameterized inputs.
- **user/** — `IUser`, `IUserConversation`, `AuthenticationFilter`, `UserContextHolder`: user management and conversation tracking.
- **file/** — `IFile`, `IUpload`: file upload/download, storage in H2 database.
- **document/** — `IDocumentRead`, `IFileDocument`: document parsing (Tika integration) for RAG.
- **tool/** — `IEmbedTool` + `DefaultEmbedTool`: tool for embedding skill content into prompts.
- **model/** — Record classes for chat requests/responses, conversations, files, knowledge, skills, MCP/tools.
- **content/** — `ContentHolder`, `ContentHolderConverter`: skill content loading from classpath resources.

### Configuration Properties

All under `spring.ai.loom.agent`:
- `defaultSystem` — default system prompt (default: skill discovery prompt)
- `init` — whether to initialize ChatClient (default: true)
- `rag.*` — RAG settings: similarityThreshold, topK, prompt templates
- `mcps` — list of MCP tool configurations with Chinese labels
- `skills` — list of skill definitions with templates, tools, and params

### Auto-Configuration

`LoomAgentConfiguration` is a Spring Boot `@AutoConfiguration` that auto-configures after all major Spring AI auto-configurations (ChatModel, EmbeddingModel, VectorStore, MCP, ChatMemory). It uses `@ConditionalOnMissingBean` extensively, allowing users to override any default bean.

### REST Endpoints (all under `/spring/ai/loom/*`)

- `POST /spring/ai/loom/stream` — SSE streaming chat endpoint
- `GET/DELETE /spring/ai/loom/conversation/{id}` — conversation management
- `POST/GET/DELETE /spring/ai/loom/file` — file upload/list/delete
- `GET/PUT/DELETE /spring/ai/loom/knowledge` — knowledge base CRUD
- `GET/PUT/DELETE /spring/ai/chat/skill` — skill library CRUD
- `GET /spring/ai/chat/loom` — list MCP tools

## Common Commands

```bash
# Build entire project
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run the test application
mvn spring-boot:run -pl spring-ai-loom-agent-test

# Build a specific module
mvn clean install -pl spring-ai-loom-agent

# Package for distribution
mvn clean package
```

## Development Notes

- The project is published to JitPack (`https://jitpack.io/#com.gitee.wb04307201/spring-ai-chat`)
- The chat UI frontend is served from static resources in `spring-ai-loom-agent/src/main/resources/META-INF/resources/`
- Database migrations are managed by Flyway in `classpath:db/loom`
- Chat memory uses JDBC-backed persistence (`JdbcChatMemoryRepository`)
- H2 is the default embedded database; users can swap to MySQL/MongoDB/Redis via Spring AI auto-configurations
- The main interface pattern: define `IXxx.java` interface in core, provide `DefaultXxx.java` implementation, wire in auto-configuration with `@ConditionalOnMissingBean` for overrideability
