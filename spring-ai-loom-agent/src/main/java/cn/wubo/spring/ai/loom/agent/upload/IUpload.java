package cn.wubo.spring.ai.loom.agent.upload;

import java.io.InputStream;

public interface IUpload {

    int upload(InputStream is, String fileName);

    int deleteUpload(String fileId);

    int[][] uploadWithKnowledge(InputStream is, String fileName, String knowledgeId);

    int deleteWithKnowledge(String fileId);
}
