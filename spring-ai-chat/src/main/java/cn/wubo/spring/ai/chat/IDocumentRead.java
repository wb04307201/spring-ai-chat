package cn.wubo.spring.ai.chat;

import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.util.List;

public interface IDocumentRead {

    public List<FileInfo> list();

    public FileInfo get(String Id);

    public List<Document> read(InputStream is, String fileName);
}
