package cn.wubo.spring.ai.loom.agent.user;

import cn.wubo.spring.ai.loom.agent.excepton.ChatUiRuntimeException;
import cn.wubo.spring.ai.loom.agent.model.UserRequestRecord;
import org.springframework.beans.factory.annotation.Value;

public class DefaultUser implements IUser {

    @Value("${spring.ai.loom.agent.user.username:username}")
    private String defaultUsername;

    @Value("${spring.ai.loom.agent.user.authentication:loom-agent-auth}")
    private String defaultAuthentication;

    @Override
    public Boolean isAutoLogin() {
        return true;
    }

    @Override
    public String login(UserRequestRecord userRequestRecord) {
        return defaultAuthentication;
    }

    @Override
    public String getUsernameByAuthentication(String authentication) {
        if (defaultAuthentication.equals(authentication)) {
            return defaultUsername;
        } else {
            throw new ChatUiRuntimeException("获取用户信息失败");
        }
    }
}
