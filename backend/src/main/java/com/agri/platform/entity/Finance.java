package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_finance")
public class Finance {
    @TableId(type = IdType.AUTO)
    private Integer financeId;
    private Integer productId;
    /** 该申请所对应的银行账号(userName)，用于银行间数据隔离：银行仅能看到本行产品的申请。
     *  申请提交时由所申请产品的 bank_user_name 冗余写入（与 realName 冗余惯例一致）。 */
    private String bankUserName;
    private String ownName;
    private String realName;
    private String phone;
    private String idNum;
    private BigDecimal money;
    private BigDecimal rate;
    private Integer repayment;
    private Integer status; // 0申请中 1已通过 2已驳回
    private String remark;
    private String purpose;          // 申请原因/融资用途
    private String repaymentSource;  // 还款来源
    private String combinationName1;
    private String combinationPhone1;
    private String combinationIdnum1;
    private String combinationName2;
    private String combinationPhone2;
    private String combinationIdnum2;
    private String fileInfo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 非数据库字段：申请所对应的融资产品（银行）名称，仅在银行工作台列表中回填展示。
     * 不参与持久化，对其它接口无副作用（默认为 null）。
     */
    @TableField(exist = false)
    private String productName;

    /**
     * 非数据库字段：申请所对应融资产品的上下架状态(0在售 1暂停供应)。
     * 农户在「我的融资/融资申请」中若该产品已被银行暂停，则据此展示「已暂停供应」提示，不参与持久化。
     */
    @TableField(exist = false)
    private Integer productStatus;

    /**
     * 非数据库字段：申请人的信用分(1-5)，仅在银行审批列表/详情中回填展示，不参与持久化。
     */
    @TableField(exist = false)
    private Integer credit;

    /**
     * 非数据库字段：该农户在售/种植的农产品名称（逗号拼接，最多5个），仅在银行智能匹配中
     * 按 realName↔tb_product.ownName 回填展示，作为「融资申请 ↔ 农产品」的对接依据，不参与持久化。
     */
    @TableField(exist = false)
    private String productNames;

    /**
     * 非数据库字段：与银行目标条件(金额/期限/用途)及农户信用的综合匹配度(0-100)，
     * 仅在银行智能匹配中回填，按其降序排列，不参与持久化。
     */
    @TableField(exist = false)
    private Integer matchScore;
}