# Spring AI Chat —— Spring AI聊天

> 为你的Spring Ai快速添加聊天界面。

[![](https://jitpack.io/v/com.gitee.wb04307201/spring-ai-chat.svg)](https://jitpack.io/#com.gitee.wb04307201/spring-ai-chat)
[![star](https://gitee.com/wb04307201/spring-ai-chat/badge/star.svg?theme=dark)](https://gitee.com/wb04307201/spring-ai-chat)
[![fork](https://gitee.com/wb04307201/spring-ai-chat/badge/fork.svg?theme=dark)](https://gitee.com/wb04307201/spring-ai-chat)
[![star](https://img.shields.io/github/stars/wb04307201/spring-ai-chat)](https://github.com/wb04307201/spring-ai-chat)
[![fork](https://img.shields.io/github/forks/wb04307201/spring-ai-chat)](https://github.com/wb04307201/spring-ai-chat)  
![MIT](https://img.shields.io/badge/License-Apache2.0-blue.svg) ![JDK](https://img.shields.io/badge/JDK-17+-green.svg) ![SpringBoot](https://img.shields.io/badge/Spring%20Boot-3+-green.svg)

## 添加聊天界面
### 添加Spring AI依赖
下面以Zhipu AI为例，添加依赖：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-zhipuai</artifactId>
</dependency>
```
添加配置：
```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY}
```

### 引入聊天依赖
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
<dependency>
    <groupId>com.github.wb04307201.spring-ai-chat</groupId>
    <artifactId>spring-ai-chat-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

启动项目 访问`http://localhost:8080//easy/ai/chat`
![img.png](img.png)

## 支持RAG
下面以Redis和Tika为例，添加依赖：
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

重启项目 访问`http://localhost:8080//easy/ai/chat`
![img_1.png](img_1.png)
出现上传文件和知识库按钮

## 支持MCP服务
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
        type: ASYNC
        stdio:
          servers-configuration: classpath:mcp-servers.json
```

[mcp-servers.json](spring-ai-chat-test/src/main/resources/mcp-servers.json)

重启项目 访问`http://localhost:8080//easy/ai/chat`
![img_2.png](img_2.png)


