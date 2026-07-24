package com.agri.platform.dto;

import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class UserLoginRequest {
    private String userName;
    private String password;
    private String role;
}
