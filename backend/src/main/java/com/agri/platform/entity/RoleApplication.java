package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色申请
 */
@Data
@TableName("tb_role_application")
public class RoleApplication {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String userName;
    /** 申请目标角色：expert / bank */
    private String targetRole;
    private String realName;
    private String phone;
    private String profession;
    private String position;
    private String belong;
    private String reason;
    /** 相关材料文件URL（资质/证明等，可选） */
    private String materials;
    /** 0待审核 1通过 2驳回 */
    private Integer status;
    private String reviewRemark;
    private String reviewer;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
