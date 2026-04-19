package cn.wubo.spring.ai.chat.model;

import cn.wubo.spring.ai.chat.ContentHolder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "spring.ai.chat.ui")
public class ChatUiProperties {

    private String defaultSystem = "每次对话都先使用 @skillContents 获取技能目录，如果有与用户意图匹配的技能,则调用 @getSkill 获取技能信息";
    private boolean init = true;
    private Rag rag = new Rag();
    private List<Tool> tools = new ArrayList<>();
    private List<Skill> skills = new ArrayList<>();

    @Data
    public static class Rag {
        private double similarityThreshold = 0.0F;
        private int topK = 4;
        private String defaultPromptTemplate = """
                Context information is below.
                
                ---------------------
                {context}
                ---------------------
                
                Given the context information and no prior knowledge, answer the query.
                
                Follow these rules:
                
                1. If the answer is not in the context, just say that you don't know.
                2. Avoid statements like "Based on the context..." or "The provided information...".
                
                Query: {query}
                
                Answer:
                """;
        private String defaultEmptyContextPromptTemplate = """
                The user query is outside your knowledge base.
                Politely inform the user that you can't answer it.
                """;
    }

    @Data
    public static class Tool {
        private String name;
        private String label;
        private String description;
        private boolean defaultSelected = true;
    }

    @Data
    public static class Skill{
        private String name;
        private String description;
        private boolean defaultPreload = true;
        private List<String> tools;
        private ContentHolder skill;
        private List<SkillParam> params;

        @Data
        public static class SkillParam {
            private String name;
            private String label;
            private ParameterType type;
            private boolean required;
            private String defaultValue;
            private String placeholder;
            private List<Option> options;

            public enum ParameterType {
                TEXT,
                SELECT,
                TEXT_AREA
            }

            @Data
            public static class Option {
                private String label;
                private String value;
            }
        }
    }
}
