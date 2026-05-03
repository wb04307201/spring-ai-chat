package cn.wubo.spring.ai.chat.model;

import org.yaml.snakeyaml.tokens.AliasToken;

import java.util.List;

public record McpRecord(String name,
                        String title,
                        String version,
                        String description,
                        boolean defaultSelected,
                        List<ToolRecord> tools) {
}
