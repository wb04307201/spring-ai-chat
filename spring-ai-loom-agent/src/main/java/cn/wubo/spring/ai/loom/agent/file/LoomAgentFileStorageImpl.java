package cn.wubo.spring.ai.loom.agent.file;

import cn.wubo.file.view.storage.IFileStorage;
import cn.wubo.file.view.storage.dto.FileStorageInfo;
import cn.wubo.spring.ai.loom.agent.excepton.LoomAgentRuntimeException;
import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class LoomAgentFileStorageImpl implements IFileStorage {

    private final IFile file;

    public LoomAgentFileStorageImpl(IFile file) {
        this.file = file;
    }

    @Override
    public FileStorageInfo upload(String fileName, byte[] content, String mimeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStorageInfo findById(String id) {
        FileRecord fileRecord = file.getById(id);
        if (fileRecord != null) {
            return new FileStorageInfo(fileRecord.id(), fileRecord.fileName(), fileRecord.size(), fileRecord.mimeType(), fileRecord.path(), "1");
        }
        throw new LoomAgentRuntimeException("文件不存在");
    }

    @Override
    public List<FileStorageInfo> list() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getContentByLocation(String location) {
        Path path = Path.of(location);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new LoomAgentRuntimeException(e);
        }
    }

    @Override
    public Boolean deleteById(String id) {
        throw new UnsupportedOperationException();
    }
}
