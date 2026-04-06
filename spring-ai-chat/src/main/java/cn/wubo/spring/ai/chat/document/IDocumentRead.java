package cn.wubo.spring.ai.chat.document;

import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.util.List;

public interface IDocumentRead {

    List<FileInfo> list();

    FileInfo get(String id);

    List<Document> read(InputStream is, String fileName);

    void delete(String id);
}
