package cn.wubo.spring.ai.chat.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SkillDocument extends ChatUiProperties.Skill {

    public SkillDocument(ChatUiProperties.Skill skill, String source) {
        super(skill.getName(), skill.getDescription(), skill.isDefaultPreload(), skill.getTools(), skill.getContent(), skill.getParams());
        this.source = source;
    }

    private String source;
}
