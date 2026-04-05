package cn.wubo.spring.ai.chat.record;

public record ToolRecord(String name,
                         String title,
                         String version,
                         String label,
                         String description,
                         boolean defaultSelected) {
}
