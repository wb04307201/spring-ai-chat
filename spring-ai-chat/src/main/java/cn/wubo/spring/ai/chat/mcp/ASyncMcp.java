package cn.wubo.spring.ai.chat.mcp;

import cn.wubo.spring.ai.chat.model.ChatUiProperties;
import cn.wubo.spring.ai.chat.model.McpRecord;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ASyncMcp extends AbstractMcp {

    private final List<ChatUiProperties.Mcp> mcpProperties;
    private final List<McpAsyncClient> mcpAsyncClients;

    public ASyncMcp(List<ChatUiProperties.Mcp> mcpProperties, List<McpAsyncClient> mcpAsyncClients) {
        this.mcpProperties = mcpProperties;
        this.mcpAsyncClients = mcpAsyncClients;
    }

    public List<McpRecord> mcps() {
        return mcpAsyncClients.stream()
                .map(mcpAsyncClient -> {
                    McpSchema.Implementation clientInfo = mcpAsyncClient.getClientInfo();
                    McpSchema.ListToolsResult listToolsResult = mcpAsyncClient.listTools().block();
                    return convertToMcpRecord(clientInfo, listToolsResult != null ? listToolsResult.tools() : List.of(), mcpProperties);
                })
                .toList();
    }

    public ToolCallbackProvider getToolCallbackProvider(List<String> mcps) {
        ToolCallbackProvider toolCallbackProvider = null;
        if (!mcpAsyncClients.isEmpty()) {
            List<McpAsyncClient> tempMcpAsyncClients = new ArrayList<>();
            for (McpAsyncClient mcpAsyncClient : mcpAsyncClients) {
                if (mcps.contains(mcpAsyncClient.getClientInfo().name())) {
                    if (mcpAsyncClient.isInitialized()) {
                        tempMcpAsyncClients.add(mcpAsyncClient);
                    } else {
                        log.warn("McpAsyncClient {} 未初始化", mcpAsyncClient.getClientInfo().name());
                    }
                }
            }
            if (!tempMcpAsyncClients.isEmpty()) {
                log.debug("McpAsyncClient {} 初始化完成", tempMcpAsyncClients.stream().map(McpAsyncClient::getClientInfo).map(McpSchema.Implementation::name).collect(Collectors.joining(",")));
                toolCallbackProvider = AsyncMcpToolCallbackProvider.builder().mcpClients(tempMcpAsyncClients).build();
            }
        }
        return toolCallbackProvider;
    }
}
