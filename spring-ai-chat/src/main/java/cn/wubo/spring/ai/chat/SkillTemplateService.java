package cn.wubo.spring.ai.chat;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能模板服务
 * 提供模板解析、参数处理、默认值设置等功能
 * @author zhaoyuaxu
 */
@Service
public class SkillTemplateService {
    
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{(\\w+)\\}");
    
    /**
     * 解析参数字符串为 Map
     * 格式：key1=value1,key2=value2
     */
    public Map<String, String> parseParameters(String paramString) {
        Map<String, String> params = new HashMap<>();
        
        if (!StringUtils.hasText(paramString)) {
            return params;
        }
        
        String[] pairs = paramString.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    params.put(key, value);
                }
            }
        }
        
        return params;
    }
    
    /**
     * 验证参数是否满足要求
     */
    public void validateParameters(Map<String, String> params, List<SkillParameter> parameterDefinitions) {
        if (parameterDefinitions == null || parameterDefinitions.isEmpty()) {
            return;
        }
        
        for (SkillParameter paramDef : parameterDefinitions) {
            String paramName = paramDef.getName();
            String paramValue = params.get(paramName);
            
            // 检查必填参数
            if (paramDef.isRequired() && !StringUtils.hasText(paramValue)) {
                throw new ChatUiRuntimeException("缺少必填参数: " + paramDef.getLabel() + "(" + paramName + ")");
            }
        }
    }
    
    /**
     * 渲染模板，将参数替换到模板中
     */
    public String renderTemplate(String template, Map<String, String> params) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        
        String result = template;
        
        // 替换所有参数
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        
        return result;
    }
    
    /**
     * 从模板中提取参数名
     */
    public List<String> extractParameterNames(String template) {
        if (!StringUtils.hasText(template)) {
            return Collections.emptyList();
        }
        
        Set<String> paramNames = new HashSet<>();
        Matcher matcher = PARAM_PATTERN.matcher(template);
        
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        
        return new ArrayList<>(paramNames);
    }
    
    /**
     * 从参数定义中提取默认参数值
     */
    public Map<String, String> getDefaultParameters(List<SkillParameter> parameterDefinitions) {
        Map<String, String> defaults = new HashMap<>();
        
        if (parameterDefinitions == null) {
            return defaults;
        }
        
        for (SkillParameter paramDef : parameterDefinitions) {
            if (StringUtils.hasText(paramDef.getDefaultValue())) {
                defaults.put(paramDef.getName(), paramDef.getDefaultValue());
            }
        }
        
        return defaults;
    }
    
    /**
     * 从Skill对象加载模板内容
     * 优先级：template字段 > skill字段
     * 
     * @param skill Skill配置对象
     * @return 模板内容字符串
     * @throws ChatUiRuntimeException 如果模板加载失败
     */
    public String loadTemplate(ChatUiProperties.Skill skill) {
        if (skill == null) {
            throw new ChatUiRuntimeException("Skill对象不能为空");
        }
        
        // 优先使用template字段
        String template = skill.getTemplate();
        
        // 如果template为空，则使用skill字段的内容
        if (!StringUtils.hasText(template) && skill.getSkill() != null) {
            template = skill.getSkill().getContent();
        }
        
        // 如果都为空，抛出异常
        if (!StringUtils.hasText(template)) {
            throw new ChatUiRuntimeException("技能 '" + skill.getName() + "' 未配置模板内容");
        }
        
        return template;
    }
}
