package cn.wubo.spring.ai.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.ai.chat.ui")
public class ChatUiProperties {
    private String defaultSystem;
    private boolean init = true;
    private Rag rag = new Rag();

    @Data
    public static class Rag{
        private double similarityThreshold = 0.8;
        private int topK = 4;
        private String template = """
            <query>

            上下文信息如下。

			---------------------
			<question_answer_context>
			---------------------
			
			如果没有上下文信息，直接回答问题

			如果有上下文信息，根据上下文信息回答问题。并遵循以下规则：
			1. 如果答案不在上下文中，则直接说明您不知道。
			2. 避免使用"根据上下文..."或"提供的信息..."之类的表述。
            """;
    }
}
