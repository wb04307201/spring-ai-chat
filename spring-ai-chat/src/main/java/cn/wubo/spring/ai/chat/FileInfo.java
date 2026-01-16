package cn.wubo.spring.ai.chat;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
