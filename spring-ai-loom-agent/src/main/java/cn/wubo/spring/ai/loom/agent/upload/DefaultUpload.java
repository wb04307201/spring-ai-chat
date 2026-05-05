package cn.wubo.spring.ai.loom.agent.upload;

import cn.wubo.spring.ai.loom.agent.document.IDocumentRead;
import cn.wubo.spring.ai.loom.agent.document.IFileDocument;
import cn.wubo.spring.ai.loom.agent.excepton.ChatUiRuntimeException;
import cn.wubo.spring.ai.loom.agent.file.IFile;
import cn.wubo.spring.ai.loom.agent.model.FileDocumentRecord;
import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.user.UserContextHolder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DefaultUpload implements IUpload {

    private final IFile file;
    private final IFileDocument fileDocument;
    private final IDocumentRead documentRead;
    private final VectorStore vectorStore;
    private static final String BASE_PATH = ".local/file";

    public DefaultUpload(IFile file, IFileDocument fileDocument, IDocumentRead documentRead, VectorStore vectorStore) {
        this.file = file;
        this.fileDocument = fileDocument;
        this.documentRead = documentRead;
        this.vectorStore = vectorStore;
    }

    private Path saveFile(InputStream is, String fileName, String username, String fileId) throws IOException {
        Path filePath = Paths.get(BASE_PATH, username, fileId, fileName);
        Files.createDirectories(filePath.getParent());
        if (Files.exists(filePath)) Files.delete(filePath);
        Files.copy(is, filePath);
        return filePath;
    }

    private void removeFile(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    @Override
    public int upload(InputStream is, String fileName) {
        String username = UserContextHolder.getCurrentUser();
        try {
            String fileId = UUID.randomUUID().toString();
            Path filePath = saveFile(is, fileName, username, fileId);
            FileRecord fileRecord = new FileRecord(
                    fileId,
                    username,
                    null,
                    fileName,
                    filePath.toFile().length(),
                    LocalDateTime.now(),
                    filePath.toString()
            );
            return file.insert(fileRecord);
        } catch (IOException e) {
            throw new ChatUiRuntimeException(e);
        }
    }

    @Override
    public int deleteUpload(String fileId) {
        FileRecord fileRecord = file.getById(fileId);
        try {
            removeFile(Paths.get(fileRecord.path()));
        } catch (IOException e) {
            throw new ChatUiRuntimeException(e);
        }
        return file.delete(fileId);
    }

    @Override
    public int[][] uploadWithKnowledge(InputStream is, String fileName, String knowledgeId) {
        String username = UserContextHolder.getCurrentUser();
        try {
            String fileId = UUID.randomUUID().toString();
            Path filePath = saveFile(is, fileName, username, fileId);
            FileRecord fileRecord = new FileRecord(
                    fileId,
                    username,
                    knowledgeId,
                    fileName,
                    filePath.toFile().length(),
                    LocalDateTime.now(),
                    filePath.toString()
            );
            file.insert(fileRecord);

            Resource resource = new FileSystemResource(filePath);
            List<Document> documents = documentRead.read(resource, knowledgeId);
            vectorStore.add(documents);

            List<FileDocumentRecord> fileDocumentRecords = documents
                    .stream()
                    .map(document -> new FileDocumentRecord(fileId, document.getId()))
                    .toList();

            return fileDocument.insert(fileDocumentRecords);
        } catch (IOException e) {
            throw new ChatUiRuntimeException(e);
        }
    }

    @Override
    public int deleteWithKnowledge(String fileId) {
        List<FileDocumentRecord> fileDocumentRecords = fileDocument.getListByFileId(fileId);
        vectorStore.delete(fileDocumentRecords.stream().map(FileDocumentRecord::documentId).toList());
        fileDocument.deleteByFileId(fileId);
        FileRecord fileRecord = file.getById(fileId);
        try {
            removeFile(Paths.get(fileRecord.path()));
        } catch (IOException e) {
            throw new ChatUiRuntimeException(e);
        }
        return file.delete(fileId);
    }
}
