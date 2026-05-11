package cn.wubo.spring.ai.loom.agent.file;

import java.io.InputStream;

public interface IUpload {

    String upload(InputStream is, String fileName);

    String uploadWithKnowledge(InputStream is, String fileName, String knowledgeId);

    int deleteWithKnowledge(String fileId);

    int deleteAllKnowledge(String knowledgeId);

    byte[] download(String fileId);
}
