# Spring AI Chat

<div align="right">
  English | <a href="README.zh-CN.md">中文</a>
</div>

> Quickly add a chat interface for your Spring Ai.

[![](https://jitpack.io/v/com.gitee.wb04307201/spring-ai-chat.svg)](https://jitpack.io/#com.gitee.wb04307201/spring-ai-chat)
[![star](https://gitee.com/wb04307201/spring-ai-chat/badge/star.svg?theme=dark)](https://gitee.com/wb04307201/spring-ai-chat)
[![fork](https://gitee.com/wb04307201/spring-ai-chat/badge/fork.svg?theme=dark)](https://gitee.com/wb04307201/spring-ai-chat)
[![star](https://img.shields.io/github/stars/wb04307201/spring-ai-chat)](https://github.com/wb04307201/spring-ai-chat)
[![fork](https://img.shields.io/github/forks/wb04307201/spring-ai-chat)](https://github.com/wb04307201/spring-ai-chat)  
![MIT](https://img.shields.io/badge/License-Apache2.0-blue.svg) ![JDK](https://img.shields.io/badge/JDK-17+-green.svg) ![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)

## Features
- 🤖 AI Chat Interface
- 📚 Knowledge Base (RAG)
- 🔧 Tools (MCP)
- 🧠 Skill Library
- ⚙️ Auto Configuration

## Quick Add Chat Interface
Here we use Zhipu AI as an example, you can replace it with other LLM dependencies as needed:
### 1. Add Chat Dependency
Add JitPack repository:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
Add dependency:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>com.gitee.wb04307201.spring-ai-chat</groupId>
        <artifactId>spring-ai-chat-spring-boot-starter</artifactId>
        <version>1.1.11</version>
    </dependency>
</dependencies>
```

### 2. Add Spring AI Dependency
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-zhipuai</artifactId>
</dependency>
```

### 3. Add Configuration
```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY}
```

### 4. Start Project
Visit `http://localhost:8080/spring/ai/chat`
![img.png](img.png)

## RAG
Here we use Redis as vector database and Tika as document parser as an example, add dependencies:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tika-document-reader</artifactId>
</dependency>
```

Add configuration:
```yaml
spring:
  ai:
    vectorstore:
      redis:
        initialize-schema: true
        index-name: custom-index
        prefix: custom-prefix
  data:
    redis:
      host: localhost
      port: 9379
      password: 123456
```

Implement [IDocumentRead.java](spring-ai-chat/src/main/java/cn/wubo/spring/ai/chat/IDocumentRead.java) interface  
For example [TikaDocumentRead.java](spring-ai-chat-test/src/main/java/cn/wubo/spring/ai/chat/TikaDocumentRead.java)

Restart project and visit `http://localhost:8080/spring/ai/chat`
![img_1.png](img_1.png)
Upload file and knowledge base buttons will appear

RAG configuration:
```yaml
spring:
  ai:
    chat:
      ui:
        rag:
          similarityThreshold: 0.50   # Similarity threshold, default 0.0
          top-k: 4                    # top-k, default 4
          defaultPromptTemplate: |
            Context information is below.

            ---------------------
            {context}
            ---------------------

            Given the context information and no prior knowledge, answer the query.

            Follow these rules:

            1. If the answer is not in the context, just say that you don't know.
            2. Avoid statements like "Based on the context..." or "The provided information...".

            Query: {query}

            Answer:
          defaultEmptyContextPromptTemplate: |
            The user query is outside your knowledge base.
            Politely inform the user that you can't answer it.
```

## MCP
Taking time MCP service as an example, add dependency:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

Add configuration:
```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers-configuration: classpath:mcp-servers.json
```

```json
//mcp-servers.json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time",
        "--local-timezone=Asia/Shanghai"
      ]
    }
  }
}
```

Restart project and visit `http://localhost:8080/spring/ai/chat`
```text
1. Current time
2. Get content from `https://www.163.com/`
3. Randomly select a news item from the previous webpage content
4. Open browser and visit `https://www.baidu.com/`
5. Enter the news from step 3 in the search box and click search
```
![img_2.png](img_2.png)
![img_3.png](img_3.png)

## Skill Library
You can create skill library based on tools by writing prompts, configuration description:
```yaml
spring:
  ai:
    chat:
      ui:
        skills:
          - name: SkillName
            tools:
              - Tool1
              - Tool2
            skill: Prompt, supports classpath, you can use {param1} in the prompt as user input parameter
```

For example, deep thinking:
Tools:
```json
{
  "mcpServers": {
    "sequential-thinking": {
      "command": "npx.cmd",
      "args": [
        "-y",
        "@modelcontextprotocol/server-sequential-thinking"
      ]
    },
    "bing-search": {
      "args": [
        "-y",
        "bing-cn-mcp"
      ],
      "command": "npx.cmd"
    }
    "fetch": {
      "args": [
        "mcp-server-fetch"
      ],
      "command": "uvx"
    }
  }
}
```
Configuration:
```yaml
spring:
  ai:
    chat:
      ui:
        skills:
          - name: "Deep Thinking"
            tools:
              - spring-ai-mcp-client - sequential-thinking
              - spring-ai-mcp-client - bing-search
              - spring-ai-mcp-client - fetch
            skill: classpath:skills/sequential-thinking.st
```
Prompt:
```text
Let's think deeply about {param1}, what practical scenarios it can be used in, requirements:
- Use sequentialthinking tool to plan all steps, thoughts and branches
- Can use bing_search tool for verification before each round of thinking
- Can use fetch tool to view details of searched webpages
- No less than 5 rounds of thinking, need to have divergent brainstorming, need to have thinking branches
- Each round needs to reflect on whether own decision is correct based on queried information results
- Return at least 10 high-value usage scenarios, explain in detail why they are valuable and how to use them
```

Restart project and visit `http://localhost:8080/spring/ai/chat`
![img_4.png](img_4.png)
![img_5.png](img_5.png)