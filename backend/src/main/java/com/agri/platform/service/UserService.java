package com.agri.platform.service;

import com.agri.platform.dto.UserLoginRequest;
import com.agri.platform.dto.UserRegisterRequest;
import com.agri.platform.entity.RoleApplication;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.RoleApplicationMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleApplicationMapper roleApplicationMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MessageService messageService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册。
     * <p>农户/买家：注册即启用，可直接登录。
     * <p>专家/银行：采用「注册即申请」——先以禁用(status=0)状态建号并落一条角色申请，
     * 携带资质材料，待管理员审核通过后启用账号方可登录。
     *
     * @return 提示信息（前端据区分为「注册成功」/「申请已提交」）
     */
    public String register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, request.getUserName());
        User existingUser = userMapper.selectOne(wrapper);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        String role = request.getRole();
        boolean needsApproval = "expert".equals(role) || "bank".equals(role);
        // 专家/银行注册需补充真实姓名；银行名称(belong)用于审核通过后写入 realName
        if (needsApproval) {
            if (request.getRealName() == null || request.getRealName().trim().isEmpty()) {
                throw new RuntimeException("请填写真实姓名");
            }
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                throw new RuntimeException("请填写手机号");
            }
            if ("bank".equals(role) && (request.getBelong() == null || request.getBelong().trim().isEmpty())) {
                throw new RuntimeException("请填写银行名称");
            }
        }

        // 创建用户
        User user = new User();
        user.setUserName(request.getUserName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setIntegral(500);
        user.setCredit(5);
        // 专家/银行：审核通过前为禁用状态，禁止登录
        user.setStatus(needsApproval ? 0 : 1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        if (needsApproval) {
            // 落一条角色申请，供管理员审核（携带资质材料）
            RoleApplication app = new RoleApplication();
            app.setUserName(request.getUserName());
            app.setTargetRole(role);
            app.setRealName(request.getRealName());
            app.setPhone(request.getPhone());
            app.setProfession(request.getProfession());
            app.setPosition(request.getPosition());
            app.setBelong(request.getBelong());
            app.setReason(request.getReason());
            app.setMaterials(request.getMaterials());
            app.setStatus(0);
            roleApplicationMapper.insert(app);
            // 通知所有启用状态的管理员：有新的专家/银行注册申请待审核
            String roleLabel = "bank".equals(role) ? "银行" : "专家";
            String displayName = "bank".equals(role)
                    ? (request.getBelong() != null && !request.getBelong().trim().isEmpty()
                        ? request.getBelong().trim() : request.getRealName())
                    : request.getRealName();
            notifyAdmins("新" + roleLabel + "注册申请待审核",
                    "用户「" + request.getUserName() + "」（" + roleLabel + "：" + displayName
                            + "）提交了注册申请，请前往「角色申请」审核。");
            return "申请已提交，请等待管理员审核通过后即可登录";
        }
        return "注册成功";
    }

    /** 通知所有启用状态的管理员（站内消息），用于注册/角色升级申请提交后提醒审核。 */
    private void notifyAdmins(String title, String content) {
        List<User> admins = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getRole, "admin").eq(User::getStatus, 1));
        for (User admin : admins) {
            messageService.send(admin.getUserName(), "system", title, content, "/admin/applications");
        }
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(UserLoginRequest request) {
        // 验证角色参数
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new RuntimeException("请选择登录角色");
        }
        // 验证角色类型
        if (!request.getRole().matches("farmer|buyer|expert|bank|admin")) {
            throw new RuntimeException("无效的角色类型");
        }

        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, request.getUserName());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证角色是否匹配
        if (!user.getRole().equals(request.getRole())) {
            throw new RuntimeException("您选择的角色与账号角色不符，请重新选择");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 检查用户状态：0 可能是「专家/银行注册待审核」或「被管理员禁用」
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号尚未启用，若您注册的是专家/银行账号，请等待管理员审核通过");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getUserName());

        // 返回用户信息和Token
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", getUserInfo(user));

        return result;
    }

    /**
     * 获取用户信息（不包含密码）
     */
    public Map<String, Object> getUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userName", user.getUserName());
        userInfo.put("realName", user.getRealName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("identityNum", user.getIdentityNum());
        userInfo.put("address", user.getAddress());
        userInfo.put("role", user.getRole());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("integral", user.getIntegral());
        userInfo.put("credit", user.getCredit());
        return userInfo;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUserName(String userName) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserName, userName);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 从Token获取用户名
     */
    public String getUserByUserNameFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUsernameFromToken(token);
    }

    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 联系人列表：供「联合贷款人」等场景选择联系人。
     * 返回除自己外、正常状态的 农户/买家/专家（银行、管理员不作为共借人），仅取 姓名/电话/用户名，不含密码。
     * @param excludeUserName 排除自己
     * @param role            可选，限定角色（如 "farmer"）；为空则返回农户/买家/专家全部
     */
    public List<User> listContacts(String excludeUserName, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq(User::getRole, role.trim());
        } else {
            wrapper.in(User::getRole, "farmer", "buyer", "expert");
        }
        wrapper.eq(User::getStatus, 1)
                .ne(User::getUserName, excludeUserName)
                .select(User::getUserName, User::getRealName, User::getPhone);
        return userMapper.selectList(wrapper);
    }

    /**
     * 修改密码
     */
    public void updatePassword(User user, String oldPassword, String newPassword) {
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
