package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_knowledge")
public class Knowledge {
    @TableId(type = IdType.AUTO)
    private Integer knowledgeId;
    private String title;
    private String content;
    private String picPath;
    private String ownName;
    private Integer status; // 1发布 0草稿
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 发布者真实姓名（非数据库列，由服务层按 ownName 批量回填，用于展示） */
    @TableField(exist = false)
    private String ownRealName;

    /** 评论数（非数据库列，由服务层按 tb_discuss 统计回填，用于展示） */
    @TableField(exist = false)
    private Integer commentCount;
}