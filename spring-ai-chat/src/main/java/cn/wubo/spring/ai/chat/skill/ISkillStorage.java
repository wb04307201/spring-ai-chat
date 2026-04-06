package cn.wubo.spring.ai.chat.skill;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;

import java.util.List;

public interface ISkillStorage {

    List<ChatUiProperties.Skill> skills();

    String getSkill(String name);

    String skillContents();
}
