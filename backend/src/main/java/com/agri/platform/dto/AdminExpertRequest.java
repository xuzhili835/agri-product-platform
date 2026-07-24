package com.agri.platform.dto;

import lombok.Data;

/**
 * 管理员新增/编辑专家请求 DTO
 */
@Data
public class AdminExpertRequest {
    /** 专家用户名（新增时必填，主键） */
    private String userName;
    /** 登录密码（新增时必填） */
    private String password;
    private String realName;
    private String phone;
    private String profession;
    private String position;
    private String belong;
}
