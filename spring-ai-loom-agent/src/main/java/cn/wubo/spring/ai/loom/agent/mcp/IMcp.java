package cn.wubo.spring.ai.loom.agent.mcp;

import cn.wubo.spring.ai.loom.agent.model.McpRecord;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

public interface IMcp {

    List<McpRecord> mcps();

    ToolCallbackProvider getToolCallbackProvider(List<String> mcps);
}
