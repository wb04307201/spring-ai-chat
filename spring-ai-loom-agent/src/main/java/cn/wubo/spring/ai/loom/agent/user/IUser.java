package cn.wubo.spring.ai.loom.agent.user;

import cn.wubo.spring.ai.loom.agent.model.UserRequestRecord;

public interface IUser {

    Boolean isAutoLogin();

    String login(UserRequestRecord userRequestRecord);

    String getUsernameByAuthentication(String authentication);


}
