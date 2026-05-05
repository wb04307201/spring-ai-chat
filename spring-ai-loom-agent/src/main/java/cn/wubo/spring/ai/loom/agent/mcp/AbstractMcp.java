package cn.wubo.spring.ai.loom.agent.mcp;

import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.McpRecord;
import cn.wubo.spring.ai.loom.agent.model.ToolRecord;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

public abstract class AbstractMcp implements IMcp {

    protected McpRecord convertToMcpRecord(McpSchema.Implementation mcpSchemaImpl, List<McpSchema.Tool> mcpSchemaTools,List<LoomAgentProperties.McpProperty> mcpProperties) {
        Optional<LoomAgentProperties.McpProperty> optionalMcp = mcpProperties.stream()
                .filter(t -> t.getName().equals(mcpSchemaImpl.name()))
                .findAny();

        return optionalMcp
                .map(mcpProperty -> buildMcpRecordWithConfig(mcpSchemaImpl, mcpSchemaTools, mcpProperty))
                .orElseGet(() -> buildMcpRecordWithoutConfig(mcpSchemaImpl, mcpSchemaTools));
    }

    private McpRecord buildMcpRecordWithConfig(McpSchema.Implementation mcpSchemaImpl,
                                               List<McpSchema.Tool> mcpSchemaTools,
                                               LoomAgentProperties.McpProperty mcpProperty) {
        List<ToolRecord> tools = mcpSchemaTools.stream()
                .map(tool -> convertToToolRecord(tool, mcpProperty))
                .toList();

        return new McpRecord(
                mcpSchemaImpl.name(),
                StringUtils.hasText(mcpProperty.getTitle()) ? mcpProperty.getTitle() : mcpSchemaImpl.title(),
                mcpSchemaImpl.version(),
                StringUtils.hasText(mcpProperty.getDescription()) ? mcpProperty.getDescription() : null,
                mcpProperty.isDefaultSelected(),
                tools
        );
    }

    private ToolRecord convertToToolRecord(McpSchema.Tool mcpSchemaTool, LoomAgentProperties.McpProperty mcpProperty) {
        return mcpProperty.getTools().stream()
                .filter(t -> t.getName().equals(mcpSchemaTool.name()))
                .findAny()
                .map(toolProperty -> new ToolRecord(
                        mcpSchemaTool.name(),
                        StringUtils.hasText(toolProperty.getDescription()) ? toolProperty.getDescription() : mcpSchemaTool.description()
                ))
                .orElseGet(() -> new ToolRecord(
                        mcpSchemaTool.name(),
                        mcpSchemaTool.description()
                ));
    }

    private McpRecord buildMcpRecordWithoutConfig(McpSchema.Implementation mcpSchemaImpl,
                                                  List<McpSchema.Tool> mcpSchemaTools) {
        List<ToolRecord> tools = mcpSchemaTools.stream()
                .map(tool -> new ToolRecord(tool.name(), tool.description()))
                .toList();

        return new McpRecord(
                mcpSchemaImpl.name(),
                mcpSchemaImpl.title(),
                mcpSchemaImpl.version(),
                null,
                false,
                tools
        );
    }
}
