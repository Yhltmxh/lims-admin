package com.shou.mcmis.security.service;

import com.shou.mcmis.security.vo.LoginVO;
import com.shou.mcmis.security.vo.UserInfoVO;

public interface AuthService {
    LoginVO login(String username, String rawPassword);
    LoginVO refresh(String accessToken, String refreshToken);
    void logout(String accessToken);
    UserInfoVO getCurrentUserInfo();
}
