package cn.wubo.spring.ai.loom.agent.chat;

import cn.wubo.spring.ai.loom.agent.file.IFile;
import cn.wubo.spring.ai.loom.agent.mcp.IMcp;
import cn.wubo.spring.ai.loom.agent.model.ChatRequestRecord;
import cn.wubo.spring.ai.loom.agent.model.UserConversationRecord;
import cn.wubo.spring.ai.loom.agent.tool.IEmbedTool;
import cn.wubo.spring.ai.loom.agent.user.IUserConversation;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Optional;

public class DefaultChat implements IChat {

    private final ChatClient chatClient;
    private final Optional<RetrievalAugmentationAdvisor> retrievalAugmentationAdvisor;
    private final IMcp mcp;
    private final IEmbedTool embedTool;
    private final IUserConversation userConversation;
    private final cn.wubo.spring.ai.loom.agent.user.IUser user;
    private final IFile file;

    public DefaultChat(ChatClient chatClient, Optional<RetrievalAugmentationAdvisor> retrievalAugmentationAdvisor, IMcp mcp, IEmbedTool embedTool, IUserConversation userConversation, cn.wubo.spring.ai.loom.agent.user.IUser user, IFile file) {
        this.chatClient = chatClient;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.mcp = mcp;
        this.embedTool = embedTool;
        this.userConversation = userConversation;
        this.user = user;
        this.file = file;
    }

    @Override
    public Flux<ChatResponse> stream(ChatRequestRecord chatRequestRecord) {
        String contextUser = UserContextHolder.getCurrentUser();
        final String username = (contextUser != null) ? contextUser : user.getUsernameByAuthentication(chatRequestRecord.authentication());
        boolean exists = userConversation.exists(new UserConversationRecord(username, chatRequestRecord.conversationId()));
        if (!exists) {
            userConversation.insert(new UserConversationRecord(username, chatRequestRecord.conversationId()));
        }

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt();

        if (StringUtils.hasText(chatRequestRecord.fileId())){
            requestSpec.user(u -> u.text(chatRequestRecord.message())
                    .media(MimeTypeUtils.IMAGE_PNG, file.getResourceById(chatRequestRecord.fileId()))).tools(embedTool);
        }else{
            requestSpec.user(chatRequestRecord.message()).tools(embedTool);
        }

        requestSpec.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatRequestRecord.conversationId()));

        if (retrievalAugmentationAdvisor.isPresent() && StringUtils.hasText(chatRequestRecord.knowledgeId())) {
            requestSpec.advisors(retrievalAugmentationAdvisor.get());
            requestSpec.advisors(advisor -> advisor.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "knowledgeId == '" + chatRequestRecord.knowledgeId() + "' && username == '" + username + "'"));
        }

        ToolCallbackProvider toolCallbackProvider = mcp.getToolCallbackProvider(chatRequestRecord.mcps());

        if (toolCallbackProvider != null) {
            requestSpec.toolCallbacks(toolCallbackProvider);
        }

        return requestSpec.stream().chatResponse();
    }
}
