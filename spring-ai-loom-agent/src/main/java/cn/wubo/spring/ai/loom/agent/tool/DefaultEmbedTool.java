package cn.wubo.spring.ai.loom.agent.tool;

import cn.wubo.spring.ai.loom.agent.file.IFile;
import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.SkillDocument;
import cn.wubo.spring.ai.loom.agent.skill.ISkillStorage;
import org.apache.tika.Tika;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefaultEmbedTool implements IEmbedTool {

    private final ISkillStorage skillStorage;
    private final IFile file;
    private final Optional<VectorStore> optionalVectorStore;

    public DefaultEmbedTool(ISkillStorage skillStorage, IFile file, Optional<VectorStore> optionalVectorStore) {
        this.skillStorage = skillStorage;
        this.file = file;
        this.optionalVectorStore = optionalVectorStore;
    }

    private static final String VECTOR_STORE_NOT_INITIALIZED = "当前没有Embedding模型，向量数据库未初始化";


    @Tool(description = "获取技能目录")
    @Override
    public String skillContents() {
        List<SkillDocument> results = skillStorage
                .list()
                .stream()
                .filter(SkillDocument::isDefaultPreload).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个技能目录:%n%n", results.size()));
        sb.append(String.format("%-10s %-50s %-50s%n", "技能名", "技能描述", "参数"));

        for (LoomAgentProperties.SkillProperty skill : results) {
            sb.append(String.format("%-10s %-50s %-50s%n", skill.getName(), skill.getDescription(), skill.getParams()));
        }

        sb.append("\n提示：使用技能名可以查询具体技能信息");
        return sb.toString();
    }

    @Tool(description = "根据技能名返回获取详细的技能信息")
    @Override
    public String getSkill(@ToolParam(description = "技能名") String name) {
        List<SkillDocument> results = skillStorage
                .list()
                .stream()
                .filter(skill -> skill.getName().equals(name))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个技能:%n%n", results.size()));

        for (LoomAgentProperties.SkillProperty skill : results) {
            sb.append(String.format("技能名:%s%n", skill.getName()));
            sb.append(String.format("技能描述:%s%n", skill.getDescription()));
            sb.append(String.format("技能内容:%s%n", skill.getContent().getText()));
            sb.append(String.format("技能参数:%s%n", skill.getParams()));
        }

        sb.append("\n提示：技能内容中类似{param}的参数定义需要用参数值补全");
        return sb.toString();
    }

    @Tool(description = "根据文件路径将文件信息进行添加进文件管理，添加成功返回文件id")
    @Override
    public String addFile(@ToolParam(description = "文件路径") String path, ToolContext toolContext) {
        Path filePath = Paths.get(path);
        if (!filePath.toFile().exists()) {
            return "文件不存在";
        }
        Tika tika = new Tika();
        String mimeType;
        try {
            mimeType = tika.detect(filePath.toFile());
        } catch (IOException e) {
            return e.getMessage();
        }
        String username = (String) toolContext.getContext().get("username");
        String fileId = UUID.randomUUID().toString();
        FileRecord fileRecord = new FileRecord(
                fileId,
                username,
                null,
                filePath.getFileName().toString(),
                filePath.toFile().length(),
                LocalDateTime.now(),
                filePath.toString(),
                "local",
                mimeType
        );
        file.insert(fileRecord);
        return fileId;
    }

    @Tool(description = "根据文件id在文件管理检查存在性，如存在则生成文件下载url")
    @Override
    public String downloadFileUrl(@ToolParam(description = "文件id") String fileId, ToolContext toolContext) {
        try {
            file.getById(fileId);
        }catch (EmptyResultDataAccessException e){
            return "文件不存在";
        }
        String baseUrl = (String) toolContext.getContext().get("baseUrl");
        return baseUrl + "/spring/ai/chat/file/download/" + fileId;
    }

    /**
    @Tool(description = "当要求智能助手记住或长期记忆什么的时候，可以使用这个工具添加记忆")
    @Override
    public String addMemory(@ToolParam(description = "记忆内容") String text, ToolContext toolContext) {
        if (!optionalVectorStore.isPresent()) {
            return VECTOR_STORE_NOT_INITIALIZED;
        }
        VectorStore vectorStore = optionalVectorStore.get();
        Document document = new Document(text);
        document.getMetadata().put("type", "memory");
        String username = (String) toolContext.getContext().get("username");
        document.getMetadata().put("username", username);
        vectorStore.add(List.of(document));
        return "记忆添加记忆成功,记忆Id:" + document.getId();
    }

    @Tool(description = "每次对话都是使用这个工具，根据用户的意图提取用于嵌入相似度比较的文本，检索智能助手的记忆")
    @Override
    public String getMemory(@ToolParam(description = "用于嵌入相似度比较的文本") String query, ToolContext toolContext) {
        if (!optionalVectorStore.isPresent()) {
            return VECTOR_STORE_NOT_INITIALIZED;
        }
        VectorStore vectorStore = optionalVectorStore.get();
        String username = (String) toolContext.getContext().get("username");
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .filterExpression("type == 'knowledge' && username == '" + username + "'")
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        if (documents.isEmpty()) {
            return "没有找到任何记忆";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(documents.size()).append(" 条记忆:\n");
            for (Document document : documents) {
                sb.append("记忆Id:").append(document.getId()).append("\n记忆内容:\n").append(document.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    @Tool(description = "当需要智能助手遗忘记忆的时候，这个工具可以根据记忆Id删除对应记忆")
    @Override
    public String removeMemory(@ToolParam(description = "记忆Id") String id) {
        if (!optionalVectorStore.isPresent()) {
            return VECTOR_STORE_NOT_INITIALIZED;
        }
        VectorStore vectorStore = optionalVectorStore.get();
        vectorStore.delete(List.of(id));
        return "删除记忆成功";
    }
    **/
}
