package cn.wubo.spring.ai.chat.model;

public record ToolRecord(String name,
                         String title,
                         String version,
                         String label,
                         String description,
                         boolean defaultSelected) {
}
