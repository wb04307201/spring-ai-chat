package cn.wubo.spring.ai.loom.agent.user;

import cn.wubo.spring.ai.loom.agent.model.UserRequestRecord;
import cn.wubo.spring.ai.loom.agent.model.UserResponseRecord;

public interface IUser {

    Boolean isAutoLogin();

    UserResponseRecord login(UserRequestRecord userRequestRecord);

    String getUsernameByAuthentication(String authentication);


}
