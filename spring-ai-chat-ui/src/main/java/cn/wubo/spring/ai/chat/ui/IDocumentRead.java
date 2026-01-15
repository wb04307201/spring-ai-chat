package cn.wubo.spring.ai.chat.ui;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.List;

public interface IDocumentRead {

    public List<FileInfo> list();

    public FileInfo get(String Id);

    public List<Document> read(InputStream is, String fileName);
}
