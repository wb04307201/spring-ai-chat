package cn.wubo.spring.ai.loom.agent.user;

import cn.wubo.spring.ai.loom.agent.model.ConversationRecord;
import cn.wubo.spring.ai.loom.agent.model.UserConversationRecord;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DefaultUserConversation implements IUserConversation {

    private final JdbcTemplate jdbcTemplate;
    private final ChatMemoryRepository chatMemoryRepository;

    public DefaultUserConversation(JdbcTemplate jdbcTemplate, ChatMemoryRepository chatMemoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.chatMemoryRepository = chatMemoryRepository;
    }

    private UserConversationRecord mapUserConversationRecord(ResultSet rs, int rowNum) throws SQLException {
        return new UserConversationRecord(
                rs.getString("username"),
                rs.getString("conversation_id")
        );
    }

    @Override
    public List<ConversationRecord> getList() {
        String username = UserContextHolder.getCurrentUser();
        List<UserConversationRecord> userConversationRecords = jdbcTemplate.query(
                "select * from user_conversation where username = ?",
                this::mapUserConversationRecord,
                username
        );

        List<ConversationRecord> conversations = new ArrayList<>();
        for (UserConversationRecord userConversationRecord : userConversationRecords) {
            List<Message> messages = chatMemoryRepository.findByConversationId(userConversationRecord.conversationId());
            String text = messages.get(0).getText();
            String preview = text.length() > 20 ? text.substring(0, 20) : text;
            conversations.add(new ConversationRecord(userConversationRecord.conversationId(), preview));
        }
        return conversations;
    }

    @Override
    public boolean exists(UserConversationRecord userConversationRecord) {
        return jdbcTemplate.queryForObject(
                "select exists(select 1 from user_conversation where username = ? and conversation_id = ?)",
                Boolean.class,
                userConversationRecord.username(),
                userConversationRecord.conversationId()
        );
    }


    @Override
    public int insert(UserConversationRecord userConversationRecord) {
        return jdbcTemplate.update(
                "insert into user_conversation (username, conversation_id) values (?, ?)",
                userConversationRecord.username(),
                userConversationRecord.conversationId()
        );
    }

    @Override
    public int delete(UserConversationRecord userConversationRecord) {
        return jdbcTemplate.update(
                "delete from user_conversation where username = ? and conversation_id = ?",
                userConversationRecord.username(),
                userConversationRecord.conversationId()
        );
    }
}
