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
- 📚 知识库(RAG)
- 🔧 工具(MCP)
- 🧠 技能库
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
        <version>1.1.14</version>
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

通过配置课题为工具添加中文名和描述以及默认选中，例如
```yaml
spring:
  ai:
    chat:
      ui:
        tools:
          - name: spring-ai-mcp-client - time
            label: 时间
            description:
              一个提供时间和时区转换功能的模型上下文协议服务。该服务使大型语言模型能够获取当前时间信息，并使用IANA时区名称进行时区转换，同时具备自动检测系统时区的功能。
            default-selected:  true
```

重启项目 访问`http://localhost:8080/spring/ai/chat`
勾选工具，输入以下内容
```text
1. 现在的时间
2. 获取`https://www.163.com/`网页内容
3. 从上一步的网页内容中随机选取获取一条新闻
4. 打开浏览器，访问`https://www.baidu.com/`地址
5. 在搜索框输入步骤3的新闻，并并点击搜索
```
![img_2.png](img_2.png)
![img_3.png](img_3.png)
![img_6.png](img_6.png)

## 技能库
可以编写技能加入技能库，技能可以配置参数与使用的工具，配置说明如下：
```yaml
spring:
  ai:
    chat:
      ui:
        skills:
          - name: 测试3
            skill: classpath:skills/test3.st  #技能描述，支持classpath test3.st内容"{param1}，现在几点了，另外你知道{prama2}吗？{param3}"
            params:
              - name: param1
                label: 参数1
                type: select # select | text | text_area 下拉框 | 文本 | 多行文本
                options: # 下拉框选项
                  - label: 你好
                    value: 你好
                  - label: Hello
                    value: Hello
                default-value: 你好 # 默认值
                required:  true # 是否必填（默认非必填）
              - name: prama2
                label: 参数2
                type: text
                required: true
                placeholder: 你想知道的事情
              - name: param3
                label: 参数3
                type: text_area
                placeholder: 补充内容，可以不输入
```

重启项目 访问`http://localhost:8080/spring/ai/chat`
![img_4.png](img_4.png)
![img_5.png](img_5.png)
