package cn.wubo.spring.ai.loom.agent.file;

import cn.wubo.spring.ai.loom.agent.model.FileRecord;

import java.util.List;

public interface IFile {

    List<FileRecord> list(String knowledgeId);

    int insert(FileRecord fileInfo);

    int delete(String id);

    FileRecord getById(String id);
}
