package com.agri.platform.dto;

import lombok.Data;

/**
 * 提问请求 DTO
 * 字段对齐 tb_question 实体（expertName 为目标专家的 userName）
 */
@Data
public class QuestionRequest {
    /** 目标专家用户名（tb_expert.user_name） */
    private String expertName;
    /** 提问者联系电话 */
    private String phone;
    /** 农作物名称 */
    private String plantName;
    /** 问题标题 */
    private String title;
    /** 问题内容（对应 tb_question.question 列） */
    private String question;
}
