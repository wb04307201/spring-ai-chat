package cn.wubo.spring.ai.chat;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TikaDocumentRead implements IDocumentRead {

    private final TokenTextSplitter tokenTextSplitter;
    private final ExtractedTextFormatter extractedTextFormatter;
    private final KeywordMetadataEnricher keywordMetadataEnricher;
    private final List<FileInfo> fileInfos = new ArrayList<>();

    public TikaDocumentRead(ChatModel chatModel) {
        // 创建一个分词器，用于将文本拆分为多个块
        this.tokenTextSplitter = new TokenTextSplitter();
        // 配置提取文本格式化器，设置各种文本处理选项
        this.extractedTextFormatter = ExtractedTextFormatter.builder().withLeftAlignment(false) // 设置左对齐选项
                .withNumberOfBottomTextLinesToDelete(0) // 设置要删除的底部文本行数
                .withNumberOfTopTextLinesToDelete(0) // 设置要删除的顶部文本行数
                .withNumberOfTopPagesToSkipBeforeDelete(0) // 设置在删除前要跳过的顶部页面数
                .build();
        // 构建关键词元数据增强器
        this.keywordMetadataEnricher = KeywordMetadataEnricher.builder(chatModel)
                .keywordCount(5) // 设置关键词数量为5个
                .build();
    }

    @Override
    public List<FileInfo> list() {
        return fileInfos.stream().map(fileInfo -> {
            FileInfo copiedFileInfo = new FileInfo();
            copiedFileInfo.setId(fileInfo.getId());
            copiedFileInfo.setFileName(fileInfo.getFileName());
            copiedFileInfo.setPath(fileInfo.getPath());
            copiedFileInfo.setSize(fileInfo.getSize());
            copiedFileInfo.setUploadTime(fileInfo.getUploadTime());
            return copiedFileInfo;
        }).toList();
    }

    @Override
    public FileInfo get(String Id) {
        return fileInfos
                .stream()
                .filter(fileInfo -> fileInfo.getId().equals(Id))
                .findAny().orElseThrow(() -> new ChatUiRuntimeException("文件不存在"));
    }

    @Override
    public List<Document> read(InputStream is, String fileName) {
        try {
            Path filePath = Paths.get("temp", System.currentTimeMillis() + "", fileName);
            Files.createDirectories(filePath.getParent());
            if (Files.exists(filePath)) Files.delete(filePath);
            Files.copy(is, filePath);
            Resource fileResource = new FileSystemResource(filePath);

            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(fileResource, extractedTextFormatter);
            List<Document> documentList = tikaDocumentReader.read();
            List<Document> tokenTextSplitterDocumentList = tokenTextSplitter.apply(documentList);
            List<Document> keywordMetadataEnricherDocumentList = keywordMetadataEnricher.apply(tokenTextSplitterDocumentList);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setId(UUID.randomUUID().toString());
            fileInfo.setFileName(fileName);
            fileInfo.setPath(filePath.toString());
            fileInfo.setSize(fileResource.contentLength());
            fileInfo.setDocumentIds(keywordMetadataEnricherDocumentList.stream().map(Document::getId).toList());
            fileInfo.setUploadTime(LocalDateTime.now());
            fileInfos.add(fileInfo);

            return keywordMetadataEnricherDocumentList;
        } catch (IOException e) {
            throw new ChatUiRuntimeException(e);
        }
    }
}
