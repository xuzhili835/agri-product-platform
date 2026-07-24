package com.agri.platform.service;

import com.agri.platform.dto.QuestionRequest;
import com.agri.platform.entity.Question;
import com.agri.platform.entity.QuestionReply;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface QuestionService {
    void askQuestion(String userName, QuestionRequest request);
    Page<Question> getQuestionList(int page, int pageSize);
    Page<Question> getQuestionList(int page, int pageSize, Integer status, String questioner, String expertName);
    Question getQuestionById(Integer questionId);
    void answerQuestion(Integer questionId, String userName, String answer);
    void closeQuestion(Integer questionId, String userName);
    void deleteQuestion(Integer questionId, String userName);

    /** 获取问题的追问回复列表（按时间正序） */
    List<QuestionReply> getReplies(Integer questionId);

    /** 追问/回答（多轮对话）：farmer 追问、expert 回答 */
    void addReply(Integer questionId, String userName, String content);
}
