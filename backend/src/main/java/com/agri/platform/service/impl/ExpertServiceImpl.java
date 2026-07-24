package com.agri.platform.service.impl;

import com.agri.platform.dto.ExpertRequest;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.Question;
import com.agri.platform.entity.Reserve;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.ExpertMapper;
import com.agri.platform.mapper.QuestionMapper;
import com.agri.platform.mapper.ReserveMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.ExpertService;
import com.agri.platform.service.ReserveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ExpertServiceImpl implements ExpertService {

    @Autowired
    private ExpertMapper expertMapper;

    @Autowired
    private ReserveMapper reserveMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ReserveService reserveService;

    @Override
    public void updateExpert(String userName, ExpertRequest request) {
        Expert expert = expertMapper.selectById(userName);
        if (expert == null) {
            throw new RuntimeException("专家不存在");
        }
        if (request.getRealName() != null) expert.setRealName(request.getRealName());
        if (request.getPhone() != null) expert.setPhone(request.getPhone());
        if (request.getProfession() != null) expert.setProfession(request.getProfession());
        if (request.getPosition() != null) expert.setPosition(request.getPosition());
        if (request.getBelong() != null) expert.setBelong(request.getBelong());
        expertMapper.updateById(expert);
    }

    @Override
    public Expert getExpertByUserName(String userName) {
        return expertMapper.selectById(userName);
    }

    @Override
    public Page<Reserve> getReserveList(String expertName, int page, int pageSize, Integer status) {
        // 读取前惰性清理超期预约（专家侧入口）
        reserveService.sweepOverdue();
        Page<Reserve> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Reserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reserve::getExpertName, expertName);
        if (status != null) {
            wrapper.eq(Reserve::getStatus, status);
        }
        wrapper.orderByDesc(Reserve::getCreateTime);
        return reserveMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void confirmReserve(Integer reserveId, String expertName, Integer status) {
        confirmReserve(reserveId, expertName, status, null);
    }

    @Override
    public void confirmReserve(Integer reserveId, String expertName, Integer status, String answer) {
        Reserve reserve = reserveMapper.selectById(reserveId);
        if (reserve == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!reserve.getExpertName().equals(expertName)) {
            throw new RuntimeException("无权限处理此预约");
        }
        reserve.setStatus(status);
        if (answer != null && !answer.isEmpty()) {
            reserve.setAnswer(answer);
        }
        reserveMapper.updateById(reserve);
    }

    @Override
    public List<Expert> getAllExperts() {
        List<Expert> experts = expertMapper.selectList(null);
        fillDisplayInfo(experts);
        return experts;
    }

    /**
     * 批量回填专家展示信息：头像 来自 tb_user，已回答数 来自 tb_question。
     * 这些均为非数据库列（@TableField(exist=false)），仅用于前端展示，避免卡片出现假数据/空头像。
     * 注：专家不涉及借款，信用分(credit) 对其无意义，故不回填、前端也不展示星级。
     */
    private void fillDisplayInfo(List<Expert> experts) {
        if (experts == null || experts.isEmpty()) return;

        List<String> userNames = experts.stream()
                .map(Expert::getUserName)
                .filter(Objects::nonNull)
                .filter(n -> !n.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (userNames.isEmpty()) return;

        // 一次性查出相关用户的头像
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getUserName, userNames)
        );
        Map<String, User> userMap = new HashMap<>();
        for (User u : users) {
            userMap.put(u.getUserName(), u);
        }

        // 一次性查出这些专家"实际回答过"的问题数：以 answer 非空为准。
        // answer 在专家每次回复时写入(recordReply)，不依赖 status——避免已回答的问题被提问者
        // 关闭后 status 由 1→2 而漏计（这正是首页回答数恒为 0 的根因）。
        List<Question> answered = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .in(Question::getExpertName, userNames)
                        .isNotNull(Question::getAnswer)
                        .ne(Question::getAnswer, "")
        );
        Map<String, Long> answerCountMap = answered.stream()
                .filter(q -> q.getExpertName() != null)
                .collect(Collectors.groupingBy(Question::getExpertName, Collectors.counting()));

        for (Expert e : experts) {
            User u = userMap.get(e.getUserName());
            if (u != null) {
                e.setAvatar(u.getAvatar());
            }
            e.setAnswerCount(answerCountMap.getOrDefault(e.getUserName(), 0L).intValue());
        }
    }
}
