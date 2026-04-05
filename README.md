# Spring AI Chat

<div align="right">
  English | <a href="README.zh-CN.md">中文</a>
</div>

> Quickly add a chat interface to your Spring AI application.

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
- 🧠 Skills Library
- ⚙️ Auto Configuration

## Quick Start: Add Chat Interface
The following example uses Zhipu AI. You can replace it with other LLM dependencies as needed:

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
            <version>1.1.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>com.gitee.wb04307201.spring-ai-chat</groupId>
        <artifactId>spring-ai-chat-spring-boot-starter</artifactId>
        <version>1.1.12</version>
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

### 4. Start the Project
Visit `http://localhost:8080/spring/ai/chat`
![img.png](img.png)

## RAG (Retrieval-Augmented Generation)
The following example uses Redis as the vector database and Tika as the document parsing tool. Add dependencies:
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

Implement the [IDocumentRead.java](spring-ai-chat/src/main/java/cn/wubo/spring/ai/chat/IDocumentRead.java) interface  
For example: [TikaDocumentRead.java](spring-ai-chat-test/src/main/java/cn/wubo/spring/ai/chat/TikaDocumentRead.java)

Restart the project and visit `http://localhost:8080/spring/ai/chat`
![img_1.png](img_1.png)
File upload and knowledge base buttons will appear.

RAG configuration:
```yaml
spring:
  ai:
    chat:
      ui:
        rag:
          similarityThreshold: 0.50   # Similarity threshold, default 0.0
          top-k: 4                    # Top-k results, default 4
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

## MCP (Model Context Protocol)
Taking the time MCP service as an example, add dependency:
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

Configure Chinese labels, descriptions, and default selection for tools:
```yaml
spring:
  ai:
    chat:
      ui:
        tools:
          - name: spring-ai-mcp-client - time
            label: Time
            description:
              A Model Context Protocol service that provides time and timezone conversion functionality. This service enables large language models to obtain current time information and perform timezone conversions using IANA timezone names, with automatic system timezone detection.
            default-selected:  true
```

Restart the project and visit `http://localhost:8080/spring/ai/chat`
Check the tool and input the following content:
```text
1. Current time
2. Get webpage content from `https://www.163.com/`
3. Randomly select one news item from the previous step's webpage content
4. Open browser and visit `https://www.baidu.com/`
5. Input the news from step 3 into the search box and click search
```
![img_2.png](img_2.png)
![img_3.png](img_3.png)
![img_6.png](img_6.png)

## Skills Library
You can write skills and add them to the skills library. Skills can be configured with parameters and tools. Configuration example:
```yaml
spring:
  ai:
    chat:
      ui:
        skills:
          - name: Test 3
            skill: classpath:skills/test3.st  # Skill description, supports classpath. Content of test3.st: "{param1}, what time is it now, also do you know {prama2}? {param3}"
            params:
              - name: param1
                label: Parameter 1
                type: select # select | text | text_area (dropdown | text | multi-line text)
                options: # Dropdown options
                  - label: Hello
                    value: Hello
                  - label: Hello
                    value: Hello
                default-value: Hello # Default value
                required:  true # Whether required (default: optional)
              - name: prama2
                label: Parameter 2
                type: text
                required: true
                placeholder: What you want to know
              - name: param3
                label: Parameter 3
                type: text_area
                placeholder: Additional content, optional
```

Restart the project and visit `http://localhost:8080/spring/ai/chat`
![img_4.png](img_4.png)
![img_5.png](img_5.png)
