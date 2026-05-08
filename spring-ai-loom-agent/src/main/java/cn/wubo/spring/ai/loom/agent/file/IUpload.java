package cn.wubo.spring.ai.loom.agent.file;

import java.io.InputStream;

public interface IUpload {

    String upload(InputStream is, String fileName);

    int deleteUpload(String fileId);

    String uploadWithKnowledge(InputStream is, String fileName, String knowledgeId);

    int deleteWithKnowledge(String fileId);
}
