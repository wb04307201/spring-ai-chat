package cn.wubo.spring.ai.chat.record;

import java.util.List;

public record SkillRequest(String skillName,
                           String params,
                           List<String> tools,
                           String conversationId) {
}
