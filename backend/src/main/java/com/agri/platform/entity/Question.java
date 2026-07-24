package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String expertName;
    private String expertRealName;
    private String questioner;
    private String questionerRealName;
    private String phone;
    private String plantName;
    private String title;
    private String question;
    private String answer;
    private Integer status; // 0未回答 1已回答 2已关闭
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}