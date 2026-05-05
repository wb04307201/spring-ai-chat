package cn.wubo.spring.ai.loom.agent.file;

import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

public class DefaultFile implements IFile {

    private final JdbcTemplate jdbcTemplate;

    public DefaultFile(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FileRecord> list(String knowledgeId) {
        String username = UserContextHolder.getCurrentUser();
        if (StringUtils.hasText(knowledgeId)) {
            return jdbcTemplate.query(
                    "SELECT * FROM file_info WHERE knowledge_id = ? AND username = ?",
                    new BeanPropertyRowMapper<>(FileRecord.class),
                    knowledgeId,
                    username
            );
        } else {
            return jdbcTemplate.query(
                    "SELECT * FROM file_info WHERE knowledge_id IS NULL AND username = ?",
                    new BeanPropertyRowMapper<>(FileRecord.class),
                    username
            );
        }
    }

    @Override
    public int insert(FileRecord fileInfo) {
        return jdbcTemplate.update(
                "INSERT INTO file_info (id, file_name, size, upload_time, path) VALUES (?, ?, ?, ?, ?)",
                fileInfo.id(),
                fileInfo.fileName(),
                fileInfo.size(),
                fileInfo.uploadTime(),
                fileInfo.path()
        );
    }

    @Override
    public int delete(String id) {
        return jdbcTemplate.update(
                "DELETE FROM file_info WHERE id = ?",
                id
        );
    }

    @Override
    public FileRecord getById(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM file_info WHERE id = ?",
                new BeanPropertyRowMapper<>(FileRecord.class),
                id
        );
    }
}
