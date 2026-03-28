# Spring AI Chat —— Spring AI聊天

<div align="right">
  <a href="README.md">English</a> | 中文
</div>

> 为你的Spring Ai快速添加聊天界面。

[![](https://jitpack.io/v/com.gitee.wb04307201/spring-ai-chat.svg)](https://jitpack.io/#com.gitee.wb04307201/spring-ai-chat)
[![star](https://gitee.com/wb04307201/spring-ai-chat/badge/star.svg?theme=dark)](https://gitee.com/wb04307201/spring-ai-chat)
[![fork](https://gitee.com/wb04307201/spring-ai-chat/badge/fork.svg?theme=dark)](https://gitee.com/wb04307201/spring-ai-chat)
[![star](https://img.shields.io/github/stars/wb04307201/spring-ai-chat)](https://github.com/wb04307201/spring-ai-chat)
[![fork](https://img.shields.io/github/forks/wb04307201/spring-ai-chat)](https://github.com/wb04307201/spring-ai-chat)  
![MIT](https://img.shields.io/badge/License-Apache2.0-blue.svg) ![JDK](https://img.shields.io/badge/JDK-17+-green.svg) ![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)

## 功能特性
- 🤖 AI聊天界面
- 🧠 RAG支持
- 🛠 MCP支持
- ⚙️ 自动配置

## 快速添加聊天界面
下面以Zhipu AI为例进行说明，可以按需替换成其它大语言模型依赖：
### 1.引入聊天依赖
增加 JitPack 仓库：
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
引入依赖；
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
        <version>1.1.10</version>
    </dependency>
</dependencies>
```

### 2. 添加Spring AI依赖
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-zhipuai</artifactId>
</dependency>
```

### 3. 添加配置
```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY}
```

### 4. 启动项目
访问`http://localhost:8080/spring/ai/chat`
![img.png](img.png)

## RAG
下面以Redis作为向量数据库和Tika作为文档拆解工具为例，添加依赖：
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

添加配置：
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

实现[IDocumentRead.java](spring-ai-chat/src/main/java/cn/wubo/spring/ai/chat/IDocumentRead.java)接口  
例如[TikaDocumentRead.java](spring-ai-chat-test/src/main/java/cn/wubo/spring/ai/chat/TikaDocumentRead.java)

重启项目 访问`http://localhost:8080/spring/ai/chat`
![img_1.png](img_1.png)
出现上传文件和知识库按钮

rag配置如下：
```yaml
spring:
  ai:
    chat:
      ui:
        rag:
          similarityThreshold: 0.50   # 相似度阈值,默认0.0
          top-k: 4                    # top-k，默认4
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
以时间MCP服务为例，添加依赖：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

添加配置：
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

重启项目 访问`http://localhost:8080/spring/ai/chat`
```text
1. 现在的时间
2. 获取`https://www.163.com/`网页内容
3. 从上一步的网页内容中随机选取获取一条新闻
4. 打开浏览器，访问`https://www.baidu.com/`地址
5. 在搜索框输入步骤3的新闻，并并点击搜索
```
![img_2.png](img_2.png)
![img_3.png](img_3.png)

## 技能库
可以依据工具编写提示词形成技能库，配置说明
```yaml
spring:
  ai:
    chat:
      ui:
        skills:
          - name: 技能名
            tools:
              - 工具1
              - 工具2
            skill: 提示词，支持classpath，可在在提示词中使用{param1}作为用户输入参数
```

例如深入思考：
工具：
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
配置：
```yaml
spring:
  ai:
    chat:
      ui:
        skills:
          - name: "深入思考"
            tools:
              - spring-ai-mcp-client - sequential-thinking
              - spring-ai-mcp-client - bing-search
              - spring-ai-mcp-client - fetch
            skill: classpath:skills/sequential-thinking.st
```
提示词：
```text
来深入思考一下，{param1}可以用于什么实际场景当中，要求：
- 使用sequentialthinking工具来规划所有的步骤，思考和分支
- 可以使用bing_search工具进行搜索，每一轮Thinking之前都先搜索验证
- 可以用fetch工具来查看搜索到的网页详情
- 思考轮数不低于5轮，且需要有发散脑暴意识，需要有思考分支
- 每一轮需要根据查询的信息结果，反思自己的决策是否正确
- 返回至少10个高价值的使用场景，并详细说明为什么价值高，如何用
```

重启项目 访问`http://localhost:8080/spring/ai/chat`
![img_4.png](img_4.png)
![img_5.png](img_5.png)
