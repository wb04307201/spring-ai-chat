package cn.wubo.spring.ai.chat.tool;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import cn.wubo.spring.ai.chat.model.SkillDocument;
import cn.wubo.spring.ai.chat.skill.ISkillStorage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

public class DefaultEmbedTool implements IEmbedTool {

    private final ISkillStorage skillStorage;

    public DefaultEmbedTool(ISkillStorage skillStorage) {
        this.skillStorage = skillStorage;
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

        for (ChatUiProperties.Skill skill : results) {
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

        for (ChatUiProperties.Skill skill : results) {
            sb.append(String.format("技能名:%s%n", skill.getName()));
            sb.append(String.format("技能描述:%s%n", skill.getDescription()));
            sb.append(String.format("技能内容:%s%n", skill.getContent().getText()));
            sb.append(String.format("技能参数:%s%n", skill.getParams()));
        }

        sb.append("\n提示：技能内容中类似{param}的参数定义需要用参数值补全");
        return sb.toString();
    }
}
