package cn.wubo.spring.ai.chat.skill;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import cn.wubo.spring.ai.chat.model.SkillContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Override
    public List<ChatUiProperties.Skill> skills() {
        return skills;
    }

    @Tool(description = "根据技能名返回技能信息")
    @Override
    public String getSkill(@ToolParam(description = "技能名") String name) {
        try {
            return objectMapper.writeValueAsString(skills.stream().filter(skill -> skill.getName().equals(name)).toList());
        } catch (JsonProcessingException e) {
            log.warn("技能信息转换失败", e);
            return e.getMessage();
        }
    }


    @Tool(description = "获取技能目录")
    @Override
    public String skillContents() {
        try {
            return objectMapper.writeValueAsString(skills.stream()
                    .filter(ChatUiProperties.Skill::isPreloading)
                    .map(skill -> new SkillContent(skill.getName(), skill.getDescription(), skill.getParams()))
                    .toList());
        } catch (JsonProcessingException e) {
            log.warn("技能内容转换失败", e);
            return e.getMessage();
        }
    }
}
