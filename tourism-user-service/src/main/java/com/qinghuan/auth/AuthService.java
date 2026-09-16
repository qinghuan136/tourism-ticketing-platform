package com.qinghuan.auth;

import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.entity.UserAccount;

public interface AuthService {

    /** 注册游客账号。 */
    void register(RegisterDTO registerDTO);

    /** 校验账号密码并签发 JWT。 */
    String login(UserAccount userAccount);
}
