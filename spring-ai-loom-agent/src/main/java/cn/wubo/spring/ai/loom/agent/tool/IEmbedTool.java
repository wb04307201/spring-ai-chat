package cn.wubo.spring.ai.loom.agent.tool;

import org.springframework.ai.chat.model.ToolContext;

public interface IEmbedTool {

    String skillContents();

    String getSkill(String name);

    String addFile(String path, ToolContext toolContext);

    String downloadFileUrl(String fileId, ToolContext toolContext);
}
