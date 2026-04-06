package cn.wubo.spring.ai.chat.model;

import java.util.List;

public record SkillContent(
        String name,
        String description,
        List<ChatUiProperties.Skill.SkillParam> params
) {
}
