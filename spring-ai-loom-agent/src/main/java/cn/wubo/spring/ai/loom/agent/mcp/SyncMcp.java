package cn.wubo.spring.ai.loom.agent.mcp;

import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.McpRecord;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SyncMcp extends AbstractMcp {

    private final List<LoomAgentProperties.McpProperty> mcpProperties;
    private final List<McpSyncClient> mcpSyncClients;

    public SyncMcp(List<LoomAgentProperties.McpProperty> mcpProperties, List<McpSyncClient> mcpSyncClients) {
        this.mcpProperties = mcpProperties;
        this.mcpSyncClients = mcpSyncClients;
    }

    public List<McpRecord> mcps() {
        return mcpSyncClients.stream()
                .filter(McpSyncClient::isInitialized)
                .map(mcpSyncClient -> convertToMcpRecord(
                        mcpSyncClient.getClientInfo(),
                        mcpSyncClient.listTools().tools(),
                        mcpProperties
                ) )
                .toList();
    }

    public ToolCallbackProvider getToolCallbackProvider(List<String> mcps) {
        ToolCallbackProvider toolCallbackProvider = null;
        if (!mcpSyncClients.isEmpty() && mcps != null && !mcps.isEmpty()) {
            List<McpSyncClient> tempMcpSyncClients = new ArrayList<>();
            for (McpSyncClient mcpSyncClient : mcpSyncClients) {
                if (mcps.contains(mcpSyncClient.getClientInfo().name())) {
                    if (mcpSyncClient.isInitialized()) {
                        tempMcpSyncClients.add(mcpSyncClient);
                    } else {
                        log.warn("McpSyncClient {} 未初始化", mcpSyncClient.getClientInfo().name());
                    }
                }
            }
            if (!tempMcpSyncClients.isEmpty()) {
                log.debug("McpSyncClient {} 初始化完成", tempMcpSyncClients.stream().map(McpSyncClient::getClientInfo).map(McpSchema.Implementation::name).collect(Collectors.joining(",")));
                toolCallbackProvider = SyncMcpToolCallbackProvider.builder().mcpClients(tempMcpSyncClients).build();
            }
        }
        return toolCallbackProvider;
    }
}
