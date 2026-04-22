package cn.wubo.spring.ai.chat.skill;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import cn.wubo.spring.ai.chat.model.SkillDocument;

import java.util.List;

public interface ISkillStorage {

    List<SkillDocument> list();

    void save(ChatUiProperties.Skill skill);

    SkillDocument get(String name);

    void remove(String name);
}
