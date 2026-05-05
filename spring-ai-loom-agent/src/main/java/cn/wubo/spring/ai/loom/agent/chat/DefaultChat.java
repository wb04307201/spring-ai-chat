package cn.wubo.spring.ai.loom.agent.chat;

import cn.wubo.spring.ai.loom.agent.mcp.IMcp;
import cn.wubo.spring.ai.loom.agent.model.ChatRequestRecord;
import cn.wubo.spring.ai.loom.agent.tool.IEmbedTool;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import reactor.core.publisher.Flux;

import java.util.Optional;

public class DefaultChat implements IChat {

    private final ChatClient chatClient;
    private final Optional<RetrievalAugmentationAdvisor> retrievalAugmentationAdvisor;
    private final IMcp mcp;
    private final IEmbedTool embedTool;

    public DefaultChat(ChatClient chatClient, Optional<RetrievalAugmentationAdvisor> retrievalAugmentationAdvisor, IMcp mcp, IEmbedTool embedTool) {
        this.chatClient = chatClient;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.mcp = mcp;
        this.embedTool = embedTool;
    }

    @Override
    public Flux<ChatResponse> stream(ChatRequestRecord chatRequestRecord) {
        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt().user(chatRequestRecord.message()).tools(embedTool);
        requestSpec.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatRequestRecord.conversationId()));

        if (retrievalAugmentationAdvisor.isPresent() && chatRequestRecord.enableRag()) {
            requestSpec.advisors(retrievalAugmentationAdvisor.get());
            String username = UserContextHolder.getCurrentUser();
            requestSpec.advisors(advisor -> advisor.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "knowledgeId == '" + chatRequestRecord.knowledgeId() + "' && username == '" + username + "'"));
        }

        ToolCallbackProvider toolCallbackProvider = mcp.getToolCallbackProvider(chatRequestRecord.mcps());

        if (toolCallbackProvider != null) {
            requestSpec.toolCallbacks(toolCallbackProvider);
        }

        return requestSpec.stream().chatResponse();
    }
}
