package com.agri.platform.service.impl;

import com.agri.platform.entity.Finance;
import com.agri.platform.entity.FinanceProduct;
import com.agri.platform.entity.JointInvitation;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.FinanceMapper;
import com.agri.platform.mapper.FinanceProductMapper;
import com.agri.platform.mapper.JointInvitationMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.JointInvitationService;
import com.agri.platform.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JointInvitationServiceImpl implements JointInvitationService {

    @Autowired
    private JointInvitationMapper jointInvitationMapper;

    @Autowired
    private FinanceMapper financeMapper;

    @Autowired
    private FinanceProductMapper financeProductMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    @Override
    @Transactional
    public void invite(String applicantUserName, Integer financeId, String jointUserName, int slot) {
        if (jointUserName == null || jointUserName.trim().isEmpty()) {
            return;
        }
        jointUserName = jointUserName.trim();
        if (jointUserName.equals(applicantUserName)) {
            throw new RuntimeException("不能邀请自己作为联合贷款人");
        }
        User jointUser = userMapper.selectById(jointUserName);
        if (jointUser == null) {
            throw new RuntimeException("所选联合贷款人不存在");
        }
        // 联合贷款人只能是农户
        if (!"farmer".equals(jointUser.getRole())) {
            throw new RuntimeException("联合贷款人只能是农户");
        }

        JointInvitation inv = new JointInvitation();
        inv.setFinanceId(financeId);
        inv.setApplicantUserName(applicantUserName);
        inv.setJointUserName(jointUserName);
        inv.setSlot(slot);
        inv.setStatus(0);
        inv.setCreateTime(LocalDateTime.now());
        jointInvitationMapper.insert(inv);

        // 通知被邀请人
        User applicant = userMapper.selectById(applicantUserName);
        String applicantName = displayUserName(applicant, applicantUserName);
        messageService.send(jointUserName, "finance",
                "联合贷款人邀请",
                applicantName + " 邀请您作为融资申请 #" + financeId + " 的联合贷款人，请前往「我的融资」确认。",
                "/farmer/my-finance");
    }

    @Override
    public List<JointInvitation> listMine(String jointUserName) {
        LambdaQueryWrapper<JointInvitation> w = new LambdaQueryWrapper<>();
        w.eq(JointInvitation::getJointUserName, jointUserName)
                .orderByDesc(JointInvitation::getCreateTime);
        List<JointInvitation> list = jointInvitationMapper.selectList(w);
        fillDisplay(list);
        return list;
    }

    @Override
    public List<JointInvitation> listByFinance(Integer financeId) {
        LambdaQueryWrapper<JointInvitation> w = new LambdaQueryWrapper<>();
        w.eq(JointInvitation::getFinanceId, financeId)
                .orderByAsc(JointInvitation::getSlot);
        List<JointInvitation> list = jointInvitationMapper.selectList(w);
        fillDisplay(list);
        return list;
    }

    @Override
    @Transactional
    public void accept(Integer id, String jointUserName) {
        JointInvitation inv = jointInvitationMapper.selectById(id);
        if (inv == null) {
            throw new RuntimeException("邀请不存在");
        }
        if (!inv.getJointUserName().equals(jointUserName)) {
            throw new RuntimeException("无权操作此邀请");
        }
        if (inv.getStatus() != null && inv.getStatus() != 0) {
            throw new RuntimeException("该邀请已处理");
        }
        // 同意前校验：被邀请人须已填写身份证号，否则姓名/电话/身份证号无法绑定到融资申请
        User jointUser = userMapper.selectById(jointUserName);
        if (jointUser == null) {
            throw new RuntimeException("用户信息不存在");
        }
        if (jointUser.getIdentityNum() == null || jointUser.getIdentityNum().trim().isEmpty()) {
            throw new RuntimeException("请先在个人资料中填写身份证号后再同意联合贷款邀请");
        }
        inv.setStatus(1);
        inv.setHandleTime(LocalDateTime.now());
        jointInvitationMapper.updateById(inv);

        // 同意后：把联合人资料（姓名/电话/身份证号，均来自个人资料）回填到融资申请对应 slot
        Finance finance = financeMapper.selectById(inv.getFinanceId());
        if (finance != null && jointUser != null) {
            if (inv.getSlot() != null && inv.getSlot() == 2) {
                finance.setCombinationName2(jointUser.getRealName());
                finance.setCombinationPhone2(jointUser.getPhone());
                finance.setCombinationIdnum2(jointUser.getIdentityNum());
            } else {
                finance.setCombinationName1(jointUser.getRealName());
                finance.setCombinationPhone1(jointUser.getPhone());
                finance.setCombinationIdnum1(jointUser.getIdentityNum());
            }
            financeMapper.updateById(finance);
        }

        // 通知申请人：对方已同意
        String jointName = displayUserName(jointUser, jointUserName);
        messageService.send(inv.getApplicantUserName(), "finance",
                "联合贷款人已同意",
                jointName + " 已同意作为您融资申请 #" + inv.getFinanceId() + " 的联合贷款人。",
                "/farmer/my-finance");
    }

    @Override
    @Transactional
    public void decline(Integer id, String jointUserName) {
        JointInvitation inv = jointInvitationMapper.selectById(id);
        if (inv == null) {
            throw new RuntimeException("邀请不存在");
        }
        if (!inv.getJointUserName().equals(jointUserName)) {
            throw new RuntimeException("无权操作此邀请");
        }
        if (inv.getStatus() != null && inv.getStatus() != 0) {
            throw new RuntimeException("该邀请已处理");
        }
        inv.setStatus(2);
        inv.setHandleTime(LocalDateTime.now());
        jointInvitationMapper.updateById(inv);

        User jointUser = userMapper.selectById(jointUserName);
        String jointName = displayUserName(jointUser, jointUserName);
        messageService.send(inv.getApplicantUserName(), "finance",
                "联合贷款人已拒绝",
                jointName + " 拒绝了作为您融资申请 #" + inv.getFinanceId() + " 联合贷款人的邀请。",
                "/farmer/my-finance");
    }

    // ==================== 私有辅助 ====================

    /** 批量回填展示字段：申请人/联合人真实姓名、融资金额、产品名 */
    private void fillDisplay(List<JointInvitation> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 用户姓名
        Set<String> names = new HashSet<>();
        for (JointInvitation inv : list) {
            if (inv.getApplicantUserName() != null) names.add(inv.getApplicantUserName());
            if (inv.getJointUserName() != null) names.add(inv.getJointUserName());
        }
        Map<String, User> userMap = new HashMap<>();
        if (!names.isEmpty()) {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getUserName, names));
            for (User u : users) {
                userMap.put(u.getUserName(), u);
            }
        }
        // 融资申请 + 产品名
        Set<Integer> financeIds = list.stream()
                .map(JointInvitation::getFinanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Finance> financeMap = new HashMap<>();
        Map<Integer, String> productNameMap = new HashMap<>();
        if (!financeIds.isEmpty()) {
            List<Finance> finances = financeMapper.selectBatchIds(financeIds);
            for (Finance f : finances) {
                financeMap.put(f.getFinanceId(), f);
            }
            Set<Integer> productIds = finances.stream()
                    .map(Finance::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!productIds.isEmpty()) {
                List<FinanceProduct> products = financeProductMapper.selectBatchIds(productIds);
                for (FinanceProduct p : products) {
                    productNameMap.put(p.getProductId(), p.getBankName());
                }
            }
        }
        for (JointInvitation inv : list) {
            User au = userMap.get(inv.getApplicantUserName());
            inv.setApplicantRealName(au != null && au.getRealName() != null
                    ? au.getRealName() : inv.getApplicantUserName());
            User ju = userMap.get(inv.getJointUserName());
            if (ju != null) {
                inv.setJointRealName(ju.getRealName());
                inv.setJointPhone(ju.getPhone());
            }
            Finance f = financeMap.get(inv.getFinanceId());
            if (f != null) {
                inv.setAmount(f.getMoney());
                inv.setProductName(productNameMap.getOrDefault(f.getProductId(), "—"));
            }
        }
    }

    /** 展示名：优先真实姓名，缺失回退 userName */
    private String displayUserName(User user, String fallback) {
        return user != null && user.getRealName() != null ? user.getRealName() : fallback;
    }
}
