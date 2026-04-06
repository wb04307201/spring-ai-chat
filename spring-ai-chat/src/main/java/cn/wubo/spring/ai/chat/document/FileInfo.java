package cn.wubo.spring.ai.chat.document;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FileInfo {

    private String id;
    private String fileName;
    private long size;
    private LocalDateTime uploadTime;
    private String path;
    private List<String> documentIds;
}
