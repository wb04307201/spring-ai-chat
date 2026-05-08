package cn.wubo.spring.ai.loom.agent.user;

import cn.wubo.spring.ai.loom.agent.model.ConversationRecord;
import cn.wubo.spring.ai.loom.agent.model.UserConversationRecord;

import java.util.List;

public interface IUserConversation {

    List<ConversationRecord> getList();

    boolean exists(UserConversationRecord userConversationRecord);

    int insert(UserConversationRecord userConversationRecord);

    int delete(UserConversationRecord userConversationRecord);
}
