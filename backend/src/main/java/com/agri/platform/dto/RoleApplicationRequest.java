package com.agri.platform.dto;

import lombok.Data;

/**
 * 角色申请请求 DTO（农户/买家申请成为 专家/银行）
 */
@Data
public class RoleApplicationRequest {
    /** 申请目标角色：expert / bank */
    private String targetRole;
    private String realName;
    private String phone;
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
