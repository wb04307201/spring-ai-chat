package cn.wubo.spring.ai.loom.agent.user;

import cn.wubo.spring.ai.loom.agent.excepton.LoomAgentRuntimeException;
import cn.wubo.spring.ai.loom.agent.model.UserRequestRecord;
import cn.wubo.spring.ai.loom.agent.model.UserResponseRecord;

public class DefaultUser implements IUser {

    private final String defaultUsername;
    private final String defaultNickname;
    private final String token;

    public DefaultUser(String defaultUsername, String defaultNickname, String token) {
        this.defaultUsername = defaultUsername;
        this.defaultNickname = defaultNickname;
        this.token = token;
    }

    @Override
    public Boolean isAutoLogin() {
        return true;
    }

    @Override
    public UserResponseRecord login(UserRequestRecord userRequestRecord) {
        return new UserResponseRecord(token, defaultNickname);
    }

    @Override
    public String getUsernameByAuthentication(String authentication) {
        if (token.equals(authentication)) {
            return defaultUsername;
        } else {
            throw new LoomAgentRuntimeException("获取用户信息失败");
        }
    }
}
