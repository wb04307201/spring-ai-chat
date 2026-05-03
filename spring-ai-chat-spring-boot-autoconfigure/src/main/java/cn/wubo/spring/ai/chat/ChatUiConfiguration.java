package cn.wubo.spring.ai.chat;

import cn.wubo.spring.ai.chat.document.DefaultDocumentRead;
import cn.wubo.spring.ai.chat.document.IDocumentRead;
import cn.wubo.spring.ai.chat.mcp.ASyncMcp;
import cn.wubo.spring.ai.chat.mcp.IMcp;
import cn.wubo.spring.ai.chat.mcp.SyncMcp;
import cn.wubo.spring.ai.chat.model.ChatRecord;
import cn.wubo.spring.ai.chat.model.ChatResponseRecord;
import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import cn.wubo.spring.ai.chat.model.ConversationRecord;
import cn.wubo.spring.ai.chat.skill.DefaultSkillStorage;
import cn.wubo.spring.ai.chat.skill.ISkillStorage;
import cn.wubo.spring.ai.chat.tool.DefaultEmbedTool;
import cn.wubo.spring.ai.chat.tool.IEmbedTool;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.servlet.http.Part;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@AutoConfiguration
@AutoConfigureAfter(name = {
        // ChatModel
        "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration",
        "org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration",
        "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration",
        "org.springframework.ai.model.minimax.autoconfigure.MiniMaxChatAutoConfiguration",
        "org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration",
        "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.bedrock.converse.autoconfigure.BedrockConverseProxyChatAutoConfiguration",
        "org.springframework.ai.model.transformers.autoconfigure.TransformersChatAutoConfiguration",
        "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration",
        // EmbeddingModel
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
        "org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration",
        "org.springframework.ai.model.minimax.autoconfigure.MiniMaxEmbeddingAutoConfiguration",
        "org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration",
        "org.springframework.ai.model.bedrock.titan.autoconfigure.BedrockTitanEmbeddingAutoConfiguration",
        "org.springframework.ai.model.bedrock.cohere.autoconfigure.BedrockCohereEmbeddingAutoConfiguration",
        "org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration",
        "org.springframework.ai.model.vertexai.autoconfigure.embedding.VertexAiTextEmbeddingAutoConfiguration",
        "org.springframework.ai.model.vertexai.autoconfigure.embedding.VertexAiMultiModalEmbeddingAutoConfiguration",
        "org.springframework.ai.model.transformers.autoconfigure.TransformersEmbeddingModelAutoConfiguration",
        "org.springframework.ai.model.postgresml.autoconfigure.PostgresMlEmbeddingAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration",
        // VectorStore
        "org.springframework.ai.vectorstore.azure.autoconfigure.AzureVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.cosmosdb.autoconfigure.CosmosDBVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.cassandra.autoconfigure.CassandraVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.couchbase.autoconfigure.CouchbaseSearchVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.gemfire.autoconfigure.GemFireVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.mariadb.autoconfigure.MariaDbStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.mongodb.autoconfigure.MongoDBAtlasVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.neo4j.autoconfigure.Neo4jVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.observation.autoconfigure.VectorStoreObservationAutoConfiguration",
        "org.springframework.ai.vectorstore.opensearch.autoconfigure.OpenSearchVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.oracle.autoconfigure.OracleVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.pinecone.autoconfigure.PineconeVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.typesense.autoconfigure.TypesenseVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.weaviate.autoconfigure.WeaviateVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.s3.autoconfigure.S3VectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.infinispan.autoconfigure.InfinispanVectorStoreAutoConfiguration",
        "org.springframework.ai.vectorstore.bedrockknowledgebase.autoconfigure.BedrockKnowledgeBaseVectorStoreAutoConfiguration",
        // ChatMemory
        "org.springframework.ai.model.chat.memory.redis.autoconfigure.RedisChatMemoryAutoConfiguration",
        "org.springframework.ai.model.chat.memory.repository.cassandra.autoconfigure.CassandraChatMemoryRepositoryAutoConfiguration",
        "org.springframework.ai.model.chat.memory.repository.cosmosdb.autoconfigure.CosmosDBChatMemoryRepositoryAutoConfiguration",
        "org.springframework.ai.model.chat.memory.repository.jdbc.autoconfigure.JdbcChatMemoryRepositoryAutoConfiguration",
        "org.springframework.ai.model.chat.memory.repository.mongo.autoconfigure.MongoChatMemoryRepositoryAutoConfiguration",
        "org.springframework.ai.model.chat.memory.repository.neo4j.autoconfigure.Neo4jChatMemoryRepositoryAutoConfiguration",
        // MCP
        "org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration",
        "org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration",
        "org.springframework.ai.mcp.client.common.autoconfigure.annotations.McpClientAnnotationScannerAutoConfiguration"})
@EnableConfigurationProperties({ChatUiProperties.class})
@Slf4j
public class ChatUiConfiguration {

    @Bean
    public ContentHolderConverter contentHolderConverter(ResourceLoader resourceLoader) {
        return new ContentHolderConverter(resourceLoader);
    }

    @Bean
    public ChatMemory jdbChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).build();
    }

    @ConditionalOnProperty(name = "spring.ai.chat.ui.init", havingValue = "true", matchIfMissing = true)
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, ChatUiProperties properties) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (properties.getDefaultSystem() != null) builder.defaultSystem(properties.getDefaultSystem());
        builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                new SimpleLoggerAdvisor() // logger advisor
        );
        return builder.build();
    }

    @ConditionalOnBean(EmbeddingModel.class)
    @ConditionalOnMissingBean(VectorStore.class)
    @Bean
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @ConditionalOnBean(VectorStore.class)
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore, ChatUiProperties properties) {
        return RetrievalAugmentationAdvisor.builder().documentRetriever(VectorStoreDocumentRetriever.builder().similarityThreshold(properties.getRag().getSimilarityThreshold()).topK(properties.getRag().getTopK()).vectorStore(vectorStore).build()).queryAugmenter(ContextualQueryAugmenter.builder().promptTemplate(PromptTemplate.builder().template(properties.getRag().getDefaultPromptTemplate()).build()).emptyContextPromptTemplate(PromptTemplate.builder().template(properties.getRag().getDefaultEmptyContextPromptTemplate()).build()).allowEmptyContext(true).build() // RAG advisor
        ).build();
    }

    @ConditionalOnMissingBean(ISkillStorage.class)
    @Bean
    public ISkillStorage skillStorage(ChatUiProperties properties) {
        return new DefaultSkillStorage(properties.getSkills());
    }

    @ConditionalOnMissingBean(IEmbedTool.class)
    @Bean
    public IEmbedTool embedTool(ISkillStorage skillStorage) {
        return new DefaultEmbedTool(skillStorage);
    }

    @ConditionalOnProperty(name = "spring.ai.mcp.client.stdio", havingValue = "ASYNC")
    @Bean
    public IMcp aSyncMcp(ChatUiProperties properties, List<McpAsyncClient> mcpAsyncClients) {
        return new ASyncMcp(properties.getMcps(), mcpAsyncClients);
    }

    @ConditionalOnMissingBean
    @Bean
    public IMcp syncMcp(ChatUiProperties properties, List<McpSyncClient> mcpSyncClients) {
        return new SyncMcp(properties.getMcps(), mcpSyncClients);
    }

    @Bean("wb04307201ChatUiRouter")
    public RouterFunction<ServerResponse> chatUiRouter() {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("spring/ai/chat", request -> ServerResponse.temporaryRedirect(URI.create("/spring/ai/chat/index.html")).build());
        return builder.build();
    }

    @Bean("wb04307201ChatUiMcpRouter")
    public RouterFunction<ServerResponse> chatUiMcpRouter(IMcp mcp) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("spring/ai/chat", request -> ServerResponse.temporaryRedirect(URI.create("/spring/ai/chat/index.html")).build());
        builder.GET("spring/ai/chat/mcp", request -> ServerResponse.ok().body(mcp.mcps()));
        return builder.build();
    }

    @Bean("wb04307201ChatUiSkillRouter")
    public RouterFunction<ServerResponse> chatUiSkillRouter(ISkillStorage skillStorage) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("spring/ai/chat/skill", request -> ServerResponse.ok().body(skillStorage.list()));
        builder.PUT("spring/ai/chat/skill", request -> {
            ChatUiProperties.Skill skill = request.body(ChatUiProperties.Skill.class);
            skillStorage.save(skill);
            return ServerResponse.ok().body(true);
        });
        builder.GET("spring/ai/chat/skill/{name}", request -> {
            String name = request.pathVariable("name");
            return ServerResponse.ok().body(skillStorage.get(name));
        });
        builder.DELETE("spring/ai/chat/skill/{name}", request -> {
            String name = request.pathVariable("name");
            skillStorage.remove(name);
            return ServerResponse.ok().body(true);
        });
        return builder.build();
    }

    @Bean("wb04307201ChatUiMemoryRouter")
    public RouterFunction<ServerResponse> chatUiMemoryRouter(JdbcChatMemoryRepository chatMemoryRepository) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("spring/ai/chat/memory", request -> {
            List<String> conversationIds = chatMemoryRepository.findConversationIds();
            List<ConversationRecord> conversations = new ArrayList<>();
            for (String conversationId : conversationIds) {
                List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);
                String text = messages.get(0).getText();
                String preview = text.length() > 20 ? text.substring(0, 20) : text;
                conversations.add(new ConversationRecord(conversationId, preview));
            }
            return ServerResponse.ok().body(conversations);
        });
        builder.GET("spring/ai/chat/memory/{conversationId}", request -> {
            String conversationId = request.pathVariable("conversationId");
            return ServerResponse.ok().body(chatMemoryRepository.findByConversationId(conversationId));
        });
        builder.DELETE("spring/ai/chat/memory/{conversationId}", request -> {
            String conversationId = request.pathVariable("conversationId");
            chatMemoryRepository.deleteByConversationId(conversationId);
            return ServerResponse.ok().body(true);
        });
        return builder.build();
    }

    @Slf4j
    @Data
    @RequiredArgsConstructor
    @RestController
    @RequestMapping
    public static class SseController {

        private final ChatClient chatClient;
        private final Optional<RetrievalAugmentationAdvisor> retrievalAugmentationAdvisor;
        private final IMcp mcp;
        private final IEmbedTool embedTool;

        @PostMapping(value = "/spring/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter stream(@RequestBody ChatRecord chatRecord) {
            // 1. 显式设置超时时间（单位毫秒），0 表示永不超时
            SseEmitter emitter = new SseEmitter(0L);

            // 2. 设置超时回调，防止连接泄露
            emitter.onTimeout(() -> {
                log.debug("SSE 链接超时");
                emitter.complete();
            });
            emitter.onCompletion(() -> log.debug("SSE 链接完成"));
            emitter.onError(e -> log.debug("SSE 链接错误：{}", e.getMessage()));


            // 3. 在异步线程中处理 AI 请求，避免阻塞 Tomcat 线程
            CompletableFuture.runAsync(() -> {
                try {
                    // 4. 订阅 Flux 流并将数据发送给 emitter
                    ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt().user(chatRecord.message()).tools(embedTool);

                    if (retrievalAugmentationAdvisor.isPresent() && chatRecord.enableRag())
                        requestSpec.advisors(retrievalAugmentationAdvisor.get());

                    requestSpec.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatRecord.conversationId()));

                    ToolCallbackProvider toolCallbackProvider = mcp.getToolCallbackProvider(chatRecord.mcps());

                    if (toolCallbackProvider != null) {
                        requestSpec.toolCallbacks(toolCallbackProvider);
                    }

                    requestSpec.stream().chatResponse().subscribe(chatResponse -> {
                        try {
                            String reasoningContent = (String) chatResponse.getResult().getOutput().getMetadata().get("reasoningContent");
                            emitter.send(new ChatResponseRecord(chatResponse.getResult().getOutput().getText(), reasoningContent), MediaType.APPLICATION_JSON);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }, emitter::completeWithError, emitter::complete);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });

            return emitter;
        }
    }

    @ConditionalOnBean(VectorStore.class)
    @ConditionalOnMissingBean(IDocumentRead.class)
    @Bean
    public IDocumentRead defaultDocumentRead(ChatModel chatModel) {
        return new DefaultDocumentRead(chatModel);
    }

    @ConditionalOnBean(VectorStore.class)
    @Bean("wb04307201ChatUiDocumentRouter")
    public RouterFunction<ServerResponse> chatUiDocumentRouter(VectorStore vectorStore, IDocumentRead documentRead) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("/spring/ai/chat/upload", request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(true));
        builder.POST("/spring/ai/chat/upload", request -> {
            Part part = request.multipartData().getFirst("file");
            if (part == null) {
                throw new IllegalArgumentException("上传的文件不能为空，请检查请求参数中是否包含名为'file'的文件");
            }
            List<Document> list = documentRead.read(part.getInputStream(), part.getSubmittedFileName());
            vectorStore.add(list);

            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(true);
        });
        builder.GET("/spring/ai/chat/knowledge", request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(documentRead.list()));
        builder.DELETE("/spring/ai/chat/knowledge/{id}", request -> {
            String id = request.pathVariable("id");
            vectorStore.delete(documentRead.get(id).getDocumentIds());
            documentRead.delete(id);
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(true);
        });
        return builder.build();
    }
}
