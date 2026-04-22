package cn.wubo.spring.ai.chat.skill;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import cn.wubo.spring.ai.chat.model.SkillDocument;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class DefaultSkillStorage implements ISkillStorage {

    private final List<SkillDocument> skills;

    public DefaultSkillStorage(List<ChatUiProperties.Skill> skills) {
        this.skills = skills.stream().map(skill -> new SkillDocument(skill, "embed")).collect(Collectors.toList());
    }

    @Override
    public List<SkillDocument> list() {
        return skills;
    }

    @Override
    public void save(ChatUiProperties.Skill skill) {
        int index = -1;
        SkillDocument skillDocument = new SkillDocument(skill, "user");
        for (int i = 0; i < skills.size(); i++) {
            if (skills.get(i).getName().equals(skillDocument.getName())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // 名称存在，替换
            skills.set(index, skillDocument);
            log.info("技能已更新: {}", skill.getName());
        } else {
            if ("embed".equals(skillDocument.getSource())) {
                // 嵌入技能已存在
                throw new IllegalArgumentException("嵌入技能已存在: " + skill.getName());
            } else {
                // 名称不存在，添加
                skills.add(skillDocument);
                log.info("技能已添加: {}", skill.getName());
            }
        }
    }

    @Override
    public SkillDocument get(String name) {
        Optional<SkillDocument> skillDocumentOptional = skills.stream().filter(skill -> skill.getName().equals(name)).findAny();
        if (skillDocumentOptional.isPresent()) {
            return skillDocumentOptional.get();
        } else {
            throw new IllegalArgumentException("技能不存在: " + name);
        }
    }

    @Override
    public void remove(String name) {
        boolean removed = skills.removeIf(skill -> skill.getName().equals(name));
        if (removed) {
            log.info("技能已删除: {}", name);
        } else {
            throw new IllegalArgumentException("技能不存在: " + name);
        }
    }
}
