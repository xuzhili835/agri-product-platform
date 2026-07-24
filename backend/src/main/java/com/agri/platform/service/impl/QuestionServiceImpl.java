package com.agri.platform.service.impl;

import com.agri.platform.dto.QuestionRequest;
import com.agri.platform.entity.Question;
import com.agri.platform.entity.QuestionReply;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.QuestionMapper;
import com.agri.platform.mapper.QuestionReplyMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.MessageService;
import com.agri.platform.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionReplyMapper replyMapper;

    @Autowired
    private MessageService messageService;

    @Override
    public void askQuestion(String userName, QuestionRequest request) {
        if (request.getExpertName() == null || request.getExpertName().isEmpty()) {
            throw new RuntimeException("请选择提问的专家");
        }
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new RuntimeException("请填写问题标题");
        }
        Question question = new Question();
        // expert_name 存储目标专家的 userName（与 tb_expert.user_name 对齐）
        question.setExpertName(request.getExpertName());
        question.setQuestioner(userName);
        question.setPhone(request.getPhone());
        question.setPlantName(request.getPlantName());
        question.setTitle(request.getTitle());
        question.setQuestion(request.getQuestion());
        question.setStatus(0); // 未回答
        // 冗余真实姓名，避免列表/详情显示登录账号
        User asker = userMapper.selectById(userName);
        if (asker != null) {
            question.setQuestionerRealName(asker.getRealName());
        }
        User expertUser = userMapper.selectById(request.getExpertName());
        if (expertUser != null) {
            question.setExpertRealName(expertUser.getRealName());
        }
        questionMapper.insert(question);

        // 通知被提问的专家：收到新提问（专家此前无任何通知，只能主动翻查，体验差）
        String askerDisplay = asker != null && asker.getRealName() != null ? asker.getRealName() : userName;
        messageService.send(request.getExpertName(), "question",
                "您有新的提问",
                askerDisplay + " 向您发起了提问「" + (request.getTitle() != null ? request.getTitle() : "") + "」",
                "/question/" + question.getId());
    }

    @Override
    public Page<Question> getQuestionList(int page, int pageSize) {
        return getQuestionList(page, pageSize, null, null, null);
    }

    @Override
    public Page<Question> getQuestionList(int page, int pageSize, Integer status, String questioner, String expertName) {
        Page<Question> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Question::getStatus, status);
        }
        if (questioner != null && !questioner.isEmpty()) {
            wrapper.eq(Question::getQuestioner, questioner);
        }
        if (expertName != null && !expertName.isEmpty()) {
            // expert_name 存储的是专家 userName，使用精确匹配
            wrapper.eq(Question::getExpertName, expertName);
        }
        wrapper.orderByDesc(Question::getCreateTime);
        return questionMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Question getQuestionById(Integer questionId) {
        return questionMapper.selectById(questionId);
    }

    @Override
    public void answerQuestion(Integer questionId, String userName, String answer) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("问题不存在");
        }
        // 仅被指派的专家本人或管理员可回答（旧接口兼容，统一走对话回复写入）
        User user = userMapper.selectById(userName);
        boolean isAdmin = user != null && "admin".equals(user.getRole());
        if (!userName.equals(question.getExpertName()) && !isAdmin) {
            throw new RuntimeException("无权回答此问题");
        }
        recordReply(question, userName, "expert", answer);
    }

    @Override
    public void closeQuestion(Integer questionId, String userName) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("问题不存在");
        }
        if (!question.getQuestioner().equals(userName)) {
            throw new RuntimeException("无权限关闭此问题");
        }
        question.setStatus(2); // 已关闭
        questionMapper.updateById(question);
    }

    @Override
    public void deleteQuestion(Integer questionId, String userName) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("问题不存在");
        }
        // 仅提问者本人或管理员可删除
        User user = userMapper.selectById(userName);
        boolean isAdmin = user != null && "admin".equals(user.getRole());
        if (!userName.equals(question.getQuestioner()) && !isAdmin) {
            throw new RuntimeException("无权删除此问题");
        }
        // 同步删除追问回复
        LambdaQueryWrapper<QuestionReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionReply::getQuestionId, questionId);
        replyMapper.delete(wrapper);
        questionMapper.deleteById(questionId);
    }

    @Override
    public List<QuestionReply> getReplies(Integer questionId) {
        LambdaQueryWrapper<QuestionReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionReply::getQuestionId, questionId);
        wrapper.orderByAsc(QuestionReply::getCreateTime);
        return replyMapper.selectList(wrapper);
    }

    @Override
    public void addReply(Integer questionId, String userName, String content) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("问题不存在");
        }
        if (question.getStatus() != null && question.getStatus() == 2) {
            throw new RuntimeException("问题已关闭，无法继续回复");
        }
        // 判定角色：提问者追问 / 被指派专家回答
        String role;
        if (userName.equals(question.getQuestioner())) {
            role = "farmer";
        } else if (userName.equals(question.getExpertName())) {
            role = "expert";
        } else {
            throw new RuntimeException("无权回复此问题");
        }
        recordReply(question, userName, role, content);
    }

    /**
     * 写入一条回复；专家回复时同步刷新 question.answer（列表预览）并在首答时置为已回答。
     */
    private void recordReply(Question question, String userName, String role, String content) {
        User author = userMapper.selectById(userName);
        String realName = author != null ? author.getRealName() : null;

        QuestionReply reply = new QuestionReply();
        reply.setQuestionId(question.getId());
        reply.setAuthorUserName(userName);
        reply.setAuthorRealName(realName);
        reply.setAuthorRole(role);
        reply.setContent(content);
        replyMapper.insert(reply);

        if ("expert".equals(role)) {
            question.setAnswer(content);
            if (question.getStatus() == null || question.getStatus() == 0) {
                question.setStatus(1); // 已回答
            }
            if (question.getExpertRealName() == null && realName != null) {
                question.setExpertRealName(realName);
            }
            questionMapper.updateById(question);
            // 通知提问者：专家回复了你的提问
            String expertDisplay = realName != null ? realName : userName;
            String title = question.getTitle() != null ? question.getTitle() : "";
            messageService.send(question.getQuestioner(), "question",
                    "专家回复了你的提问",
                    expertDisplay + " 回复了你的提问「" + title + "」",
                    "/question/" + question.getId());
        }
    }
}
