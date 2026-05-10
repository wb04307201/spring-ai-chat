package cn.wubo.spring.ai.loom.agent.file;

import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DefaultFile implements IFile {

    private final JdbcTemplate jdbcTemplate;

    public DefaultFile(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private FileRecord mapFileRecord(ResultSet rs, int rowNum) throws SQLException {
        return new FileRecord(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("knowledge_id"),
                rs.getString("file_name"),
                rs.getLong("size"),
                rs.getTimestamp("upload_time") != null ? rs.getTimestamp("upload_time").toLocalDateTime() : null,
                rs.getString("path")
        );
    }

    @Override
    public List<FileRecord> list(String knowledgeId) {
        String username = UserContextHolder.getCurrentUser();
        if (StringUtils.hasText(knowledgeId)) {
            return jdbcTemplate.query(
                    "SELECT * FROM file_info WHERE knowledge_id = ? AND username = ?",
                    this::mapFileRecord,
                    knowledgeId,
                    username
            );
        } else {
            return jdbcTemplate.query(
                    "SELECT * FROM file_info WHERE knowledge_id IS NULL AND username = ?",
                    this::mapFileRecord,
                    username
            );
        }
    }

    @Override
    public int insert(FileRecord fileInfo) {
        return jdbcTemplate.update(
                "INSERT INTO file_info (id, username, knowledge_id, file_name, size, upload_time, path) VALUES (?, ?, ?, ?, ?, ?, ?)",
                fileInfo.id(),
                fileInfo.username(),
                fileInfo.knowledgeId(),
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
                this::mapFileRecord,
                id
        );
    }

    @Override
    public Resource getResourceById(String id) {
        FileRecord fileRecord = getById(id);
        return new FileSystemResource(fileRecord.path());
    }
}
