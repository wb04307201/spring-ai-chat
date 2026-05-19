package cn.wubo.spring.ai.loom.agent.tool;

import cn.wubo.spring.ai.loom.agent.file.IFile;
import cn.wubo.spring.ai.loom.agent.model.FileRecord;
import cn.wubo.spring.ai.loom.agent.model.LoomAgentProperties;
import cn.wubo.spring.ai.loom.agent.model.SkillDocument;
import cn.wubo.spring.ai.loom.agent.skill.ISkillStorage;
import org.apache.tika.Tika;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultEmbedTool implements IEmbedTool {

    private static final String FILE_NOT_FOUND_MESSAGE = "文件信息不存在，可调用 @addFile {path:文件路径} 添加本地文件到文件管理";
    private static final String FILE_DELETED_MESSAGE = "文件不存在，已删除文件管理中文件信息";

    private final ISkillStorage skillStorage;
    private final IFile file;
    private final Map<String, String> usageMap = new HashMap<>();

    public DefaultEmbedTool(ISkillStorage skillStorage, IFile file) {
        this.skillStorage = skillStorage;
        this.file = file;
        usageMap.put("add", "本地添加文件");
        usageMap.put("conversation", "会话文件");
        usageMap.put("knowledge", "知识库文件");
    }


    @Tool(description = "获取技能目录")
    @Override
    public String skillContents() {
        List<SkillDocument> results = skillStorage
                .list()
                .stream()
                .filter(SkillDocument::isDefaultPreload).toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("技能目录（包含 %d 个技能）:%n%n", results.size()));
        sb.append(String.format("%-10s %-50s %-50s%n", "技能名", "技能描述", "参数"));

        for (LoomAgentProperties.SkillProperty skill : results) {
            sb.append(String.format("%-10s %-50s %-50s%n", skill.getName(), skill.getDescription(), skill.getParams()));
        }

        sb.append("%n%n提示：调用 @getSkill {\"skill_name\": \"匹配的技能名\"} 获取详细信息");
        return sb.toString();
    }

    @Tool(description = "根据技能名返回获取详细的技能信息")
    @Override
    public String getSkill(@ToolParam(description = "技能名") String name) {
        List<SkillDocument> results = skillStorage
                .list()
                .stream()
                .filter(skill -> skill.getName().equals(name))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 个技能:%n%n", results.size()));

        for (LoomAgentProperties.SkillProperty skill : results) {
            sb.append(String.format("技能名:%s%n", skill.getName()));
            sb.append(String.format("技能描述:%s%n", skill.getDescription()));
            sb.append(String.format("技能内容:%s%n", skill.getContent().getText()));
            sb.append(String.format("技能参数:%s%n", skill.getParams()));
        }

        sb.append("\n提示：技能内容中类似{param}的参数定义需要用参数值补全");
        return sb.toString();
    }

    @Tool(description = """
            根据保存到本地的文件路径，将文件信息添加进文件管理，添加成功返回文件id，
            如果文件路径不存在，需要先调用  @write_file 将文件保存到本地生成文件路径后，再添加本地文件到文件管理
            """)
    @Override
    public String addFile(@ToolParam(description = "文件路径") String path, ToolContext toolContext) {
        Path filePath = Paths.get(path);
        if (!filePath.toFile().exists()) {
            return "文件不存在，可调用 @write_file 保存文件后再添加";
        }
        Path normalizedPath = filePath.toAbsolutePath().normalize();
        Path tempDir = Paths.get(".local/temp").toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(tempDir)) {
            return "仅允许添加 .local/temp 目录下的文件，请先将文件复制到该目录后再添加";
        }
        Tika tika = new Tika();
        String mimeType;
        try {
            mimeType = tika.detect(filePath.toFile());
        } catch (IOException e) {
            return e.getMessage();
        }
        String username = (String) toolContext.getContext().get("username");
        String fileId = UUID.randomUUID().toString();
        FileRecord fileRecord = new FileRecord(
                fileId,
                username,
                null,
                filePath.getFileName().toString(),
                filePath.toFile().length(),
                LocalDateTime.now(),
                filePath.toString(),
                "local",
                mimeType
        );
        file.insert(fileRecord);
        return fileId;
    }

    @Tool(description = """
            根据文件管理文件目录
            """)
    @Override
    public String getFileList(ToolContext toolContext) {
        List<FileRecord> fileRecords = file.list(null);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("文件目录（包含 %d 个文件）:%n%n", fileRecords.size()));
        sb.append(String.format("%-10s %-10s %-50s %-50s%n", "文件id", "文件名", "文件大小", "文件类型"));
        for (FileRecord fileRecord : fileRecords) {
            sb.append(String.format("%-10s %-10s %-50s %-50s%n", fileRecord.id(), fileRecord.fileName(), fileRecord.size(), fileRecord.mimeType()));
        }
        sb.append("%n%n提示：调用 @getFileInfoById {fileId:文件id} 获取文件信息");
        sb.append("%n%n提示：调用 @downloadFileUrl {fileId:文件id} 下载文件url");
        sb.append("%n%n提示：调用 @viewFileUrl {fileId:文件id} 预览文件url");
        sb.append("%n%n提示：调用 @viewHtmlUrl {fileId:文件id} 查看html文件url");
        return "";
    }

    @Tool(description = """
            根据文件id，获取文件信息
            """)
    @Override
    public String getFileInfoById(String fileId, ToolContext toolContext) {
        FileRecord fileRecord = validateFileExists(fileId);
        if (fileRecord == null) {
            return FILE_DELETED_MESSAGE;
        }
        return "文件名：" + fileRecord.fileName() + "\n" +
                "文件大小：" + fileRecord.size() + "\n" +
                "文件上传时间：" + fileRecord.uploadTime() + "\n" +
                "文件用途：" + usageMap.getOrDefault(fileRecord.usage(), "位置") + "\n" +
                "文件类型：" + fileRecord.mimeType() + "\n" +
                "文件路径：" + fileRecord.path() + "\n";
    }

    @Tool(description = """
            根据文件id，生成文件下载url，
            如果没有文件id，需要先调用 @addFile {path:文件路径} 添加本地文件到文件管理后，再下载
            """)
    @Override
    public String downloadFileUrl(@ToolParam(description = "文件id") String fileId, ToolContext toolContext) {
        FileRecord fileRecord = validateFileExists(fileId);
        if (fileRecord == null) {
            return FILE_DELETED_MESSAGE;
        }
        String baseUrl = (String) toolContext.getContext().get("baseUrl");
        return "url:" + baseUrl + "/spring/ai/chat/file/download/" + fileId + "\n" +
                "文件名：" + fileRecord.fileName() + "\n";
    }

    @Tool(description = """
            根据文件id，生成文件预览url，
            如果没文件id，需要先调用 @addFile {path:文件路径} 添加本地文件到文件管理后，再预览
            """)
    @Override
    public String viewFileUrl(@ToolParam(description = "文件id") String fileId, ToolContext toolContext) {
        FileRecord fileRecord = validateFileExists(fileId);
        if (fileRecord == null) {
            return FILE_DELETED_MESSAGE;
        }
        String baseUrl = (String) toolContext.getContext().get("baseUrl");
        return "url:" + baseUrl + "/file/view/" + fileId + "\n" +
                "文件名：" + fileRecord.fileName() + "\n" +
                "使用HTML <a> 标签展示点击打开新的标签页";
    }

    private FileRecord validateFileExists(String fileId) {
        FileRecord fileRecord;
        try {
            fileRecord = file.getById(fileId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException(FILE_NOT_FOUND_MESSAGE);
        }
        if (Files.notExists(Path.of(fileRecord.path()))) {
            file.delete(fileId);
            return null;
        }
        return fileRecord;
    }

}
