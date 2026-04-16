package cn.wubo.spring.ai.chat.skill;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

@Slf4j
@Data
@RequiredArgsConstructor
public class LocalSkillStorage implements ISkillStorage {

    private final List<ChatUiProperties.Skill> skills;

    @Override
    public List<ChatUiProperties.Skill> skills() {
        return skills;
    }

    @Tool(description = "根据技能名返回技能信息")
    @Override
    public String getSkill(@ToolParam(description = "技能名") String name) {
        List<ChatUiProperties.Skill> results = skills.stream().filter(skill -> skill.getName().equals(name)).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个技能:%n%n", results.size()));

        for (ChatUiProperties.Skill skill : results) {
            sb.append(String.format("技能名:%s%n", skill.getName()));
            sb.append(String.format("技能描述:%s%n", skill.getDescription()));
            sb.append(String.format("技能内容:%s%n", skill.getSkill().getContent()));
            sb.append(String.format("技能参数:%s%n", skill.getParams()));
        }

        sb.append("\n提示：技能内容中类似{param1}需要用参数补全");
        return sb.toString();
    }


    @Tool(description = "获取技能目录")
    @Override
    public String skillContents() {
        List<ChatUiProperties.Skill> results = skills.stream().filter(ChatUiProperties.Skill::isDefaultPreload).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个技能目录:%n%n", results.size()));
        sb.append(String.format("%-10s %-50s %-50s%n", "技能名", "技能描述","参数"));

        for (ChatUiProperties.Skill skill : results) {
            sb.append(String.format("%-10s %-50s %-50s%n", skill.getName(), skill.getDescription(),skill.getParams()));
        }

        sb.append("\n提示：使用技能名可以查询具体技能信息");
        return sb.toString();
    }
}
