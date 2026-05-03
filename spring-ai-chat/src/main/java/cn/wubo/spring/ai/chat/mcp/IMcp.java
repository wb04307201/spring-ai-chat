package cn.wubo.spring.ai.chat.mcp;

import cn.wubo.spring.ai.chat.model.McpRecord;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

public interface IMcp {

    List<McpRecord> mcps();

    ToolCallbackProvider getToolCallbackProvider(List<String> mcps);
}
