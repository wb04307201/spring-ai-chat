package cn.wubo.spring.ai.loom.agent.tool;

import org.springframework.ai.chat.model.ToolContext;

public interface IEmbedTool {

    String skillContents();

    String getSkill(String name);

    String addFile(String path, ToolContext toolContext);

    String getFileList(ToolContext toolContext);

    String getFileInfoById(String fileId, ToolContext toolContext);

    String downloadFileUrl(String fileId, ToolContext toolContext);

    String viewFileUrl(String fileId, ToolContext toolContext);
}
