package cn.wubo.spring.ai.loom.agent.tool;

public interface IEmbedTool {

    String skillContents();

    String getSkill(String name);

    String addFile(String path);
}
