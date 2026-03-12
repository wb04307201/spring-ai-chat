package cn.wubo.spring.ai.chat;

import java.util.List;

public record ChatRecord(String message, String conversationId, List<String> tools) {
}
