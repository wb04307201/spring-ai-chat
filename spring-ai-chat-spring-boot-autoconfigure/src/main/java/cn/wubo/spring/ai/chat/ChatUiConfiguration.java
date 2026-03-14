package cn.wubo.spring.ai.chat;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
import java.util.concurrent.CompletableFuture;

@AutoConfiguration
@AutoConfigureAfter(name = {
        // ChatModel
        "org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration",
        "org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration",
        "org.springframework.ai.model.bedrock.autoconfigure.BedrockAiChatAutoConfiguration",
        "org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration",
        "org.springframework.ai.model.elevenlabs.autoconfigure.ElevenLabsChatAutoConfiguration",
        "org.springframework.ai.model.google.genai.autoconfigure.GoogleGenAiChatAutoConfiguration",
        "org.springframework.ai.model.huggingface.autoconfigure.HuggingFaceChatAutoConfiguration",
        "org.springframework.ai.model.minimax.autoconfigure.MinimaxChatAutoConfiguration",
        "org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration",
        "org.springframework.ai.model.oci.genai.autoconfigure.OciGenAiChatAutoConfiguration",
        "org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.openaisdk.autoconfigure.OpenAiSdkEmbeddingAutoConfiguration",
        "org.springframework.ai.model.postgresml.autoconfigure.PostgresMlEmbeddingAutoConfiguration",
        "org.springframework.ai.model.stabilityai.autoconfigure.StabilityAiChatAutoConfiguration",
        "org.springframework.ai.model.transformers.autoconfigure.TransformersChatAutoConfiguration",
        "org.springframework.ai.model.vertexai.autoconfigure.VertexAiChatAutoConfiguration",
        "org.springframework.ai.model.zhipuai.autoconfigure.ZhipuAiChatAutoConfiguration",
        // Vectorstore
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
        "org.springframework.ai.vectorstore.weaviate.autoconfigure.WeaviateVectorStoreAutoConfiguration"
})
@EnableConfigurationProperties({ChatUiProperties.class})
@Slf4j
public class ChatUiConfiguration {

    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnProperty(name = "spring.ai.chat.ui.init", havingValue = "true", matchIfMissing = true)
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatUiProperties properties) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (properties.getDefaultSystem() != null) builder.defaultSystem(properties.getDefaultSystem());
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                new SimpleLoggerAdvisor() // logger advisor
        );
        return builder.build();
    }

    @ConditionalOnBean(VectorStore.class)
    @ConditionalOnProperty(name = "spring.ai.chat.ui.init", havingValue = "true", matchIfMissing = true)
    @Bean
    public ChatClient chatClientVectorStore(ChatModel chatModel, VectorStore vectorStore, ChatUiProperties properties) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (properties.getDefaultSystem() != null) builder.defaultSystem(properties.getDefaultSystem());
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .similarityThreshold(properties.getRag().getSimilarityThreshold())
                                .topK(properties.getRag().getTopK())
                                .vectorStore(vectorStore)
                                .build())
                        .queryAugmenter(
                                ContextualQueryAugmenter.builder()
                                        .promptTemplate(
                                                PromptTemplate.builder()
                                                        .template(properties.getRag().getDefaultPromptTemplate())
                                                        .build()
                                        )
                                        .emptyContextPromptTemplate(
                                                PromptTemplate.builder()
                                                        .template(properties.getRag().getDefaultEmptyContextPromptTemplate())
                                                        .build()
                                        )
                                        .allowEmptyContext(true)
                                        .build())
                        .build(),  // RAG advisor
                SimpleLoggerAdvisor.builder().build() // logger advisor
        );
        return builder.build();
    }

    @Bean("wb04307201ChatUiRouter")
    public RouterFunction<ServerResponse> chatUiRouter(List<McpSyncClient> mcpSyncClients, List<McpAsyncClient> mcpAsyncClients) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("spring/ai/chat", request -> ServerResponse.temporaryRedirect(URI.create("/spring/ai/chat/index.html")).build());
        builder.GET("spring/ai/chat/tools", request -> {
            if (!mcpSyncClients.isEmpty()) {
                return ServerResponse.ok().body(mcpSyncClients.stream().map(McpSyncClient::getClientInfo));
            }
            if (!mcpAsyncClients.isEmpty()) {
                return ServerResponse.ok().body(mcpAsyncClients.stream().map(McpAsyncClient::getClientInfo));
            }
            return ServerResponse.ok().body(new ArrayList<>());

        });
        return builder.build();
    }

    @Slf4j
    @RestController
    @RequestMapping
    public static class SseController {

        private final ChatClient chatClient;
        private final List<McpSyncClient> mcpSyncClients;
        private final List<McpAsyncClient> mcpAsyncClients;

        public SseController(ChatClient chatClient, List<McpSyncClient> mcpSyncClients, List<McpAsyncClient> mcpAsyncClients) {
            this.chatClient = chatClient;
            this.mcpSyncClients = mcpSyncClients;
            this.mcpAsyncClients = mcpAsyncClients;
        }

        @PostMapping(value = "/spring/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter streamAi(@RequestBody ChatRecord chatRecord) {
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
                    ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                            .user(chatRecord.message())
                            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatRecord.conversationId()));

                    ToolCallbackProvider toolCallbackProvider = null;
                    if (!mcpSyncClients.isEmpty()) {
                        List<McpSyncClient> tempMcpSyncClients = new ArrayList<>();
                        for (McpSyncClient mcpSyncClient : mcpSyncClients) {
                            if (chatRecord.tools().contains(mcpSyncClient.getClientInfo().name())) {
                                if (mcpSyncClient.isInitialized())
                                    tempMcpSyncClients.add(mcpSyncClient);
                                else
                                    log.warn("McpSyncClient {} 未初始化", mcpSyncClient.getClientInfo().name());
                            }
                        }
                        if (!tempMcpSyncClients.isEmpty())
                            toolCallbackProvider = SyncMcpToolCallbackProvider.builder().mcpClients(tempMcpSyncClients).build();
                    }
                    if (!mcpAsyncClients.isEmpty()) {
                        List<McpAsyncClient> tempMcpAsyncClients = new ArrayList<>();
                        for (McpAsyncClient mcpAsyncClient : mcpAsyncClients) {
                            if (chatRecord.tools().contains(mcpAsyncClient.getClientInfo().name())) {
                                if (mcpAsyncClient.isInitialized())
                                    tempMcpAsyncClients.add(mcpAsyncClient);
                                else
                                    log.warn("McpAsyncClient {} 未初始化", mcpAsyncClient.getClientInfo().name());
                            }
                        }
                        if (!tempMcpAsyncClients.isEmpty())
                            toolCallbackProvider = AsyncMcpToolCallbackProvider.builder().mcpClients(tempMcpAsyncClients).build();
                    }

                    if (toolCallbackProvider != null) {
                        requestSpec.toolCallbacks(toolCallbackProvider);
                    }

                    requestSpec
                            .stream()
                            .content()
                            .subscribe(
                                    content -> {
                                        try {
                                            emitter.send(new ChatResponse(content), MediaType.APPLICATION_JSON);
                                        } catch (IOException e) {
                                            emitter.completeWithError(e);
                                        }
                                    },
                                    emitter::completeWithError,
                                    emitter::complete
                            );
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });

            return emitter;
        }
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
