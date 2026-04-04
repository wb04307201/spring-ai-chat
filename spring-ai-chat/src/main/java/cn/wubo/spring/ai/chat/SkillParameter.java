package cn.wubo.spring.ai.chat;

import lombok.Data;
import java.util.List;

@Data
public class SkillParameter {
    private String name;
    private String label;
    private String type;
    private boolean required;
    private String defaultValue;
    private String description;
    private List<String> options;
}
