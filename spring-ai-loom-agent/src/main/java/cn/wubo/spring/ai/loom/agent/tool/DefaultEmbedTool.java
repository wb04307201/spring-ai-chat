package cn.wubo.spring.ai.loom.agent.tool;

import cn.wubo.spring.ai.loom.agent.file.IFile;
import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.SkillDocument;
import cn.wubo.spring.ai.loom.agent.skill.ISkillStorage;
import org.apache.tika.Tika;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DefaultEmbedTool implements IEmbedTool {

    private final ISkillStorage skillStorage;
    private final IFile file;

    public DefaultEmbedTool(ISkillStorage skillStorage, IFile file) {
        this.skillStorage = skillStorage;
        this.file = file;
    }


    @Tool(description = "获取技能目录")
    @Override
    public String skillContents() {
        List<SkillDocument> results = skillStorage
                .list()
                .stream()
                .filter(SkillDocument::isDefaultPreload).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("技能目录（包含 %d 个技能）:%n%n", results.size()));
        sb.append(String.format("%-10s %-50s %-50s%n", "技能名", "技能描述", "参数"));

        for (LoomAgentProperties.SkillProperty skill : results) {
            sb.append(String.format("%-10s %-50s %-50s%n", skill.getName(), skill.getDescription(), skill.getParams()));
        }

        sb.append("%n%n提示：调用 @getSkill {\"skill_name\": \"匹配的技能名\"} 获取详细信息");
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

}
