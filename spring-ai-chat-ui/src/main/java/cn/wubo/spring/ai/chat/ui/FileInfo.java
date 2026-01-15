package cn.wubo.spring.ai.chat.ui;

import lombok.Data;

import java.util.List;

@Data
public class FileInfo {

    private String id;
    private String fieName;
    private String path;
    private List<String> documentIds;
}
