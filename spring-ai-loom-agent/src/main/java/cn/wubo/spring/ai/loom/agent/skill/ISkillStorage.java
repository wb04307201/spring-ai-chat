package cn.wubo.spring.ai.loom.agent.skill;

import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.SkillDocument;

import java.util.List;

public interface ISkillStorage {

    List<SkillDocument> list();

    void save(LoomAgentProperties.SkillProperty skill);

    SkillDocument get(String name);

    void remove(String name);
}
