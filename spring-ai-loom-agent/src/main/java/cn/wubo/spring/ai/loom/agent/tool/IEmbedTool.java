package cn.wubo.spring.ai.loom.agent.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.util.List;

public interface IEmbedTool {

    String skillContents();

    String getSkill(String name);

    String addFile(String path, ToolContext toolContext);

    String downloadFileUrl(String fileId, ToolContext toolContext);

    /**
    String addMemory(String text, ToolContext toolContext);

    String getMemory(String text, ToolContext toolContext);

    String removeMemory(String id);
    **/
}
