package cn.wubo.spring.ai.loom.agent.knowledge;

import cn.wubo.spring.ai.loom.agent.model.KnowledgeRecord;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

public class DefaultKnowledge implements IKnowledge {

    private final JdbcTemplate jdbcTemplate;

    public DefaultKnowledge(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<KnowledgeRecord> list() {
        String username = UserContextHolder.getCurrentUser();
        return jdbcTemplate.query(
                "SELECT * FROM knowledge where username = ?",
                new BeanPropertyRowMapper<>(KnowledgeRecord.class),
                username
        );
    }

    @Override
    public KnowledgeRecord insert(String name) {
        String username = UserContextHolder.getCurrentUser();
        KnowledgeRecord knowledgeRecord = new KnowledgeRecord(
                UUID.randomUUID().toString(),
                username,
                name
        );
        jdbcTemplate.update(
                "INSERT INTO knowledge (id, username, name) VALUES (?, ?, ?)",
                knowledgeRecord.id(),
                knowledgeRecord.username(),
                knowledgeRecord.name()
        );
        return knowledgeRecord;
    }

    @Override
    public int delete(String id) {
        return jdbcTemplate.update(
                "DELETE FROM knowledge WHERE id = ?",
                id
        );
    }

    @Override
    public KnowledgeRecord getById(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM knowledge WHERE id = ?",
                new BeanPropertyRowMapper<>(KnowledgeRecord.class),
                id
        );
    }
}
