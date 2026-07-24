package com.agri.platform.dto;

import lombok.Data;

/**
 * 管理员新增/编辑用户请求 DTO
 */
@Data
public class AdminUserRequest {
    private String userName;
    /** 新增时必填 */
    private String password;
    private String realName;
    private String phone;
    private String identityNum;
    private String address;
    /** farmer/buyer/expert/bank/admin */
    private String role;
    /** 1正常 0禁用 */
    private Integer status;
    /** 农户信用分（0-5，仅管理员可调整） */
    private Integer credit;
    // 当 role=expert 时可附带专家资料
    private String profession;
    private String position;
    private String belong;
}
