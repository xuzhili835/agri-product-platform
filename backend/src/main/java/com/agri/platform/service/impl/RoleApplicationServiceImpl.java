package com.agri.platform.service.impl;

import com.agri.platform.dto.RoleApplicationRequest;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.RoleApplication;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.ExpertMapper;
import com.agri.platform.mapper.RoleApplicationMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.MessageService;
import com.agri.platform.service.RoleApplicationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleApplicationServiceImpl implements RoleApplicationService {

    @Autowired
    private RoleApplicationMapper roleApplicationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExpertMapper expertMapper;

    @Autowired
    private MessageService messageService;

    @Override
    public void apply(String userName, RoleApplicationRequest request) {
        User user = userMapper.selectById(userName);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 仅 农户/买家 可申请升级角色
        if (!"farmer".equals(user.getRole()) && !"buyer".equals(user.getRole())) {
            throw new RuntimeException("当前角色无需申请");
        }
        String target = request.getTargetRole();
        if (!"expert".equals(target) && !"bank".equals(target)) {
            throw new RuntimeException("仅支持申请成为专家或银行角色");
        }
        // 不允许重复提交未处理的申请
        Long pending = roleApplicationMapper.selectCount(
                new LambdaQueryWrapper<RoleApplication>()
                        .eq(RoleApplication::getUserName, userName)
                        .eq(RoleApplication::getStatus, 0));
        if (pending != null && pending > 0) {
            throw new RuntimeException("您已有一条待审核的申请，请等待处理");
        }

        RoleApplication app = new RoleApplication();
        app.setUserName(userName);
        app.setTargetRole(target);
        app.setRealName(request.getRealName() != null ? request.getRealName() : user.getRealName());
        app.setPhone(request.getPhone() != null ? request.getPhone() : user.getPhone());
        app.setProfession(request.getProfession());
        app.setPosition(request.getPosition());
        app.setBelong(request.getBelong());
        app.setReason(request.getReason());
        app.setMaterials(request.getMaterials());
        app.setStatus(0);
        roleApplicationMapper.insert(app);
        // 通知所有启用状态的管理员：有新的角色升级申请待审核
        String roleLabel = "bank".equals(target) ? "银行" : "专家";
        String belong = request.getBelong();
        String detail = (belong != null && !belong.trim().isEmpty())
                ? "（" + roleLabel + "名称：" + belong.trim() + "）" : "";
        notifyAdmins("新" + roleLabel + "角色申请待审核",
                "用户「" + userName + "」（" + user.getRealName() + "）申请成为" + roleLabel + detail
                        + "，请前往「角色申请」审核。");
    }

    /** 通知所有启用状态的管理员（站内消息），用于角色升级申请提交后提醒审核。 */
    private void notifyAdmins(String title, String content) {
        List<User> admins = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "admin").eq(User::getStatus, 1));
        for (User admin : admins) {
            messageService.send(admin.getUserName(), "system", title, content, "/admin/applications");
        }
    }

    @Override
    public List<RoleApplication> listMine(String userName) {
        return roleApplicationMapper.selectList(
                new LambdaQueryWrapper<RoleApplication>()
                        .eq(RoleApplication::getUserName, userName)
                        .orderByDesc(RoleApplication::getCreateTime));
    }

    @Override
    public List<RoleApplication> listAll(Integer status) {
        LambdaQueryWrapper<RoleApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RoleApplication::getStatus, status);
        }
        wrapper.orderByDesc(RoleApplication::getCreateTime);
        return roleApplicationMapper.selectList(wrapper);
    }

    @Override
    public void review(Integer id, String reviewer, Integer status, String reviewRemark) {
        RoleApplication app = roleApplicationMapper.selectById(id);
        if (app == null) {
            throw new RuntimeException("申请不存在");
        }
        if (app.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new RuntimeException("审核状态非法");
        }

        app.setStatus(status);
        app.setReviewer(reviewer);
        app.setReviewRemark(reviewRemark);
        roleApplicationMapper.updateById(app);

        // 通过：变更用户角色并启用账号；专家需建立资料
        if (status == 1) {
            User user = userMapper.selectById(app.getUserName());
            if (user == null) {
                throw new RuntimeException("申请人不存在");
            }
            user.setRole(app.getTargetRole());
            // 注册即申请的账号此前为禁用(status=0)，审核通过后启用，方可登录
            user.setStatus(1);
            // 银行：把银行名称写入 realName（用于展示及融资产品归属回填）；专家/其它：取真实姓名
            String displayName = app.getRealName();
            if ("bank".equals(app.getTargetRole()) && app.getBelong() != null && !app.getBelong().trim().isEmpty()) {
                displayName = app.getBelong().trim();
            }
            user.setRealName(displayName != null ? displayName : user.getRealName());
            user.setPhone(app.getPhone() != null ? app.getPhone() : user.getPhone());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);

            if ("expert".equals(app.getTargetRole())) {
                // 已存在则更新，否则新建
                Expert expert = expertMapper.selectById(app.getUserName());
                if (expert == null) {
                    expert = new Expert();
                    expert.setUserName(app.getUserName());
                    expert.setRealName(user.getRealName());
                    expert.setPhone(user.getPhone());
                    expert.setProfession(app.getProfession());
                    expert.setPosition(app.getPosition());
                    expert.setBelong(app.getBelong());
                    expertMapper.insert(expert);
                } else {
                    expert.setProfession(app.getProfession());
                    expert.setPosition(app.getPosition());
                    expert.setBelong(app.getBelong());
                    expertMapper.updateById(expert);
                }
            }
        }
    }
}
