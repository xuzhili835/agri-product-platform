package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 联合贷款人邀请。
 * <p>
 * 农户申请融资时选择联系人作为联合贷款人，先落一条「待处理」邀请并通知对方；
 * 对方同意后，其姓名/电话/身份证号（来自个人资料）回填到 {@code tb_finance} 的 combination 字段。
 */
@Data
@TableName("tb_joint_invitation")
public class JointInvitation {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer financeId;
    private String applicantUserName;  // 申请人（农户）userName
    private String jointUserName;      // 被邀请的联合贷款人 userName
    private Integer slot;              // 联合人位置：1 或 2
    private Integer status;            // 0待处理 1已同意 2已拒绝
    private LocalDateTime createTime;
    private LocalDateTime handleTime;

    /* ========== 以下为非数据库字段，仅用于列表展示回填 ========== */

    @TableField(exist = false)
    private String applicantRealName;

    @TableField(exist = false)
    private String jointRealName;

    @TableField(exist = false)
    private String jointPhone;

    @TableField(exist = false)
    private BigDecimal amount;         // 关联融资申请的金额（展示给被邀请人）

    @TableField(exist = false)
    private String productName;        // 关联融资产品（银行）名称
}
