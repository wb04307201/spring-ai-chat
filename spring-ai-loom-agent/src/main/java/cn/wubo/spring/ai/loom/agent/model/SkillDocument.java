package cn.wubo.spring.ai.loom.agent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SkillDocument extends LoomAgentProperties.SkillProperty {

    public SkillDocument(LoomAgentProperties.SkillProperty skill, String source) {
        super(skill.getName(), skill.getDescription(), skill.isDefaultPreload(), skill.getTools(), skill.getContent(), skill.getParams());
        this.source = source;
    }

    private String source;
}
