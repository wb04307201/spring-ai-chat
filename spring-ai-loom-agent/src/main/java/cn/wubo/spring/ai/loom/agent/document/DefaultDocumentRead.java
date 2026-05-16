package cn.wubo.spring.ai.loom.agent.document;

import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;

import java.util.List;

public class DefaultDocumentRead implements IDocumentRead {

    private final TokenTextSplitter tokenTextSplitter;
    private final ExtractedTextFormatter extractedTextFormatter;
    private final KeywordMetadataEnricher keywordMetadataEnricher;
    private final SummaryMetadataEnricher summaryMetadataEnricher;
    private final LoomAgentProperties.RagProperty properties;

    public DefaultDocumentRead(ChatModel chatModel, LoomAgentProperties.RagProperty properties) {
        this.properties = properties;
        // 创建一个分词器，用于将文本拆分为多个块
        this.tokenTextSplitter = TokenTextSplitter.builder().build();
        // 配置提取文本格式化器，设置各种文本处理选项
        this.extractedTextFormatter = ExtractedTextFormatter.builder().build();
        // 构建关键词元数据增强器
        this.keywordMetadataEnricher = KeywordMetadataEnricher.builder(chatModel).keywordCount(5).build();
        // 构建摘要元数据增强器
        this.summaryMetadataEnricher = new SummaryMetadataEnricher(chatModel, List.of(SummaryMetadataEnricher.SummaryType.PREVIOUS, SummaryMetadataEnricher.SummaryType.CURRENT, SummaryMetadataEnricher.SummaryType.NEXT));

    }

    @Override
    public List<Document> read(Resource fileResource, String knowledgeId) {
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(fileResource, extractedTextFormatter);
        List<Document> documentList = tikaDocumentReader.read();
        List<Document> documents = tokenTextSplitter.apply(documentList);
        if (properties.isEnabledKeyword()){
            documents = keywordMetadataEnricher.apply(documents);
        }
        if (properties.isEnabledSummary()){
            documents = summaryMetadataEnricher.apply(documents);
        }
        String username = UserContextHolder.getCurrentUser();
        documents
                .forEach(document -> {
                    document.getMetadata().put("type", "knowledge");
                    document.getMetadata().put("knowledgeId", knowledgeId);
                    document.getMetadata().put("username", username);
                });

        return documents;
    }
}
