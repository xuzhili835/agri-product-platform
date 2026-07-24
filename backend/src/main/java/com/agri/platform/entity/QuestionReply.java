package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_question_reply")
public class QuestionReply {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer questionId;       // 所属问题 tb_question.id
    private String authorUserName;     // 回复人用户名
    private String authorRealName;     // 回复人真实姓名（冗余）
    private String authorRole;         // farmer 追问 / expert 回答
    private String content;
    private LocalDateTime createTime;
}
