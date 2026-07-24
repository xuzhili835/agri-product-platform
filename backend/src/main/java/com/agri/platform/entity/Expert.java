package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_expert")
public class Expert {
    @TableId(type = IdType.INPUT)
    private String userName;
    private String realName;
    private String phone;
    private String profession;
    private String position;
    private String belong;

    /** 头像（非数据库列，由服务层按 userName 关联 tb_user 回填，用于展示） */
    @TableField(exist = false)
    private String avatar;

    /** 已回答问题数（非数据库列，由服务层按 userName 统计 tb_question 已回答数回填，用于展示） */
    @TableField(exist = false)
    private Integer answerCount;
}