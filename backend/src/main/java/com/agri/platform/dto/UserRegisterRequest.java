package com.agri.platform.dto;

import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest {
    private String userName;
    private String password;
    private String realName;
    private String phone;
    private String role;  // farmer, buyer, expert, bank
    private String cfTurnstileToken;  // Cloudflare Turnstile 人机验证 token（前端验证后带回）

    // ===== 专家/银行「注册即申请」附加字段（仅 role=expert/bank 时使用） =====
    /** 专业（申请专家） */
    private String profession;
    /** 职位（申请专家） */
    private String position;
    /** 所属单位（专家）/ 银行名称（银行） */
    private String belong;
    /** 申请理由 */
    private String reason;
    /** 相关材料文件URL（资质/证明等，可选） */
    private String materials;
}
