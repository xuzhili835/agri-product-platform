package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_discuss")
public class Discuss {
    @TableId(type = IdType.AUTO)
    private Integer discussId;
    private Integer knowledgeId;
    private String ownName;
    private String content;
    private LocalDateTime createTime;

    /** 评论人真实姓名（非数据库列，由服务层按 ownName 批量回填，用于展示） */
    @TableField(exist = false)
    private String ownRealName;
}