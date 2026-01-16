package cn.wubo.spring.ai.chat;

import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

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

    @ConditionalOnMissingBean({VectorStore.class, ToolCallbackProvider.class})
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

    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnBean(ToolCallbackProvider.class)
    @ConditionalOnProperty(name = "spring.ai.chat.ui.init", havingValue = "true", matchIfMissing = true)
    @Bean
    public ChatClient chatClientToolCallbackProvider(ChatModel chatModel, ToolCallbackProvider tools, ChatUiProperties properties) {
        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultToolCallbacks(tools);
        if (properties.getDefaultSystem() != null) builder.defaultSystem(properties.getDefaultSystem());
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                new SimpleLoggerAdvisor() // logger advisor
        );
        return builder.build();
    }

    @ConditionalOnBean(VectorStore.class)
    @ConditionalOnMissingBean(ToolCallbackProvider.class)
    @ConditionalOnProperty(name = "spring.ai.chat.ui.init", havingValue = "true", matchIfMissing = true)
    @Bean
    public ChatClient chatClientVectorStore(ChatModel chatModel, VectorStore vectorStore, ChatUiProperties properties) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (properties.getDefaultSystem() != null) builder.defaultSystem(properties.getDefaultSystem());
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        PromptTemplate customPromptTemplate = PromptTemplate
                .builder()
                .renderer(
                        StTemplateRenderer
                                .builder()
                                .startDelimiterToken('<')
                                .endDelimiterToken('>')
                                .build()
                )
                .template(properties.getRag().getTemplate())
                .build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .searchRequest(
                                SearchRequest
                                        .builder()
                                        .similarityThreshold(properties.getRag().getSimilarityThreshold())
                                        .topK(properties.getRag().getTopK())
                                        .build()
                        )
                        .promptTemplate(customPromptTemplate)
                        .build(),    // RAG advisor
                SimpleLoggerAdvisor.builder().build() // logger advisor
        );
        return builder.build();
    }

    @ConditionalOnBean({VectorStore.class, ToolCallbackProvider.class})
    @ConditionalOnProperty(name = "spring.ai.chat.ui.init", havingValue = "true", matchIfMissing = true)
    @Bean
    public ChatClient chatClientVectorStoreToolCallbackProvider(ChatModel chatModel, VectorStore vectorStore, ToolCallbackProvider tools, ChatUiProperties properties) {
        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultToolCallbacks(tools);
        if (properties.getDefaultSystem() != null) builder.defaultSystem(properties.getDefaultSystem());
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();
        PromptTemplate customPromptTemplate = PromptTemplate
                .builder()
                .renderer(
                        StTemplateRenderer
                                .builder()
                                .startDelimiterToken('<')
                                .endDelimiterToken('>')
                                .build()
                )
                .template(properties.getRag().getTemplate())
                .build();
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .searchRequest(
                                SearchRequest
                                        .builder()
                                        .similarityThreshold(properties.getRag().getSimilarityThreshold())
                                        .topK(properties.getRag().getTopK())
                                        .build()
                        )
                        .promptTemplate(customPromptTemplate)
                        .build(),    // RAG advisor
                SimpleLoggerAdvisor.builder().build() // logger advisor
        );
        return builder.build();
    }

    @Bean("wb04307201ChatUiRouter")
    public RouterFunction<ServerResponse> chatUiRouter(ChatClient chatClient) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("spring/ai/chat", request -> ServerResponse.temporaryRedirect(URI.create("/spring/ai/chat/index.html")).build());

        // @formatter:off
        builder.POST("/spring/ai/chat/stream", request -> {
            ChatRecord chatRecord = request.body(ChatRecord.class);
            Flux<String> stream = chatClient
                    .prompt()
                    .user(chatRecord.message())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatRecord.conversationId()))
                    .stream()
                    .content();
            return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .body(stream.doOnError(Throwable::printStackTrace));
        });
        // @formatter:on

        return builder.build();
    }

    @ConditionalOnBean(VectorStore.class)
    @Bean("wb04307201ChatUiDocumentRouter")
    public RouterFunction<ServerResponse> chatUiDocumentRouter(VectorStore vectorStore, IDocumentRead documentRead) {
        RouterFunctions.Builder builder = RouterFunctions.route();
        builder.GET("/spring/ai/chat/upload", request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(true));
        builder.POST("/spring/ai/chat/upload", request -> {
            Part part = request.multipartData().getFirst("file");
            List<Document> list = documentRead.read(part.getInputStream(), part.getSubmittedFileName());
            vectorStore.add(list);

            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(true);
        });
        builder.GET("/spring/ai/chat/knowledge", request -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(documentRead.list()));
        builder.DELETE("/spring/ai/chat/knowledge/{id}", request -> {
            String id = request.pathVariable("id");
            vectorStore.delete(documentRead.get(id).getDocumentIds());
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(true);
        });

        return builder.build();
    }
}
