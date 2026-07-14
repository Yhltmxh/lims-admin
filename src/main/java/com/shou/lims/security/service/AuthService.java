package com.shou.lims.security.service;

import com.shou.lims.security.vo.LoginVO;
import com.shou.lims.security.vo.UserInfoVO;

public interface AuthService {
    LoginVO login(String username, String rawPassword);
    LoginVO refresh(String accessToken, String refreshToken);
    void logout(String accessToken);
    UserInfoVO getCurrentUserInfo();
}
