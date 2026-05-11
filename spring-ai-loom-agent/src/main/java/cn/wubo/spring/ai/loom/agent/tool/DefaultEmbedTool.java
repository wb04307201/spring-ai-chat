package cn.wubo.spring.ai.loom.agent.tool;

import cn.wubo.spring.ai.loom.agent.file.IFile;
import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.SkillDocument;
import cn.wubo.spring.ai.loom.agent.skill.ISkillStorage;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

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

    @Tool(description = "根据文件路径将文件信息进行存储，添加成功返回文件id")
    @Override
    public String addFile(@ToolParam(description = "文件路径") String path) {
        Path filePath = Paths.get(path);
        if (!filePath.toFile().exists()) {
            return "文件不存在";
        }
        String username = UserContextHolder.getCurrentUser();
        String fileId = UUID.randomUUID().toString();
        FileRecord fileRecord = new FileRecord(
                fileId,
                username,
                null,
                filePath.getFileName().toString(),
                filePath.toFile().length(),
                LocalDateTime.now(),
                filePath.toString(),
                "add"
        );
        file.insert(fileRecord);
        return fileId;
    }
}
