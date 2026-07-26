package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.RoleApplicationRequest;
import com.agri.platform.dto.UserLoginRequest;
import com.agri.platform.dto.UserRegisterRequest;
import com.agri.platform.entity.RoleApplication;
import com.agri.platform.entity.User;
import com.agri.platform.service.RoleApplicationService;
import com.agri.platform.service.TurnstileService;
import com.agri.platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TurnstileService turnstileService;

    @Autowired
    private RoleApplicationService roleApplicationService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody UserRegisterRequest request) {
        try {
            // 人机验证（Cloudflare Turnstile）：失败/连不上 Cloudflare 一律拒绝（fail-closed）
            if (!turnstileService.verify(request.getCfTurnstileToken())) {
                return Result.error("人机验证失败，请重试");
            }
            // 验证参数
            if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return Result.error("密码长度不能少于6位");
            }
            if (request.getRole() == null) {
                return Result.error("请选择角色类型");
            }
            // 验证角色类型：专家/银行采用「注册即申请」，需管理员审核通过后才能登录
            if (!request.getRole().matches("farmer|buyer|expert|bank")) {
                return Result.error("角色类型错误");
            }

            String message = userService.register(request);
            return Result.success(message);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody UserLoginRequest request) {
        try {
            // 验证参数
            if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return Result.error("密码长度不能少于6位");
            }
            if (request.getRole() == null || request.getRole().trim().isEmpty()) {
                return Result.error("请选择登录角色");
            }

            Map<String, Object> result = userService.login(request);
            return Result.success("登录成功", result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String userName = userService.getUserByUserNameFromToken(token);
            User user = userService.getUserByUserName(userName);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success(userService.getUserInfo(user));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/info")
    public Result<String> updateInfo(@RequestHeader("Authorization") String token,
                                     @RequestBody User userInfo) {
        try {
            String userName = userService.getUserByUserNameFromToken(token);
            User user = userService.getUserByUserName(userName);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 更新允许修改的字段
            // 注意：真实姓名（realName）不允许在此修改。它作为 tb_product.own_name 等表的"发布方"
            // 快照被冗余存储，改名会造成孤儿数据（农户查不到自己的已发布、联系农户报"发布方不存在"等）。
            // 注册时填写一次即永久锁定，如确需改名请直接改库。
            if (userInfo.getPhone() != null) {
                user.setPhone(userInfo.getPhone());
            }
            if (userInfo.getIdentityNum() != null) {
                user.setIdentityNum(userInfo.getIdentityNum());
            }
            if (userInfo.getAddress() != null) {
                user.setAddress(userInfo.getAddress());
            }
            if (userInfo.getAvatar() != null) {
                user.setAvatar(userInfo.getAvatar());
            }

            userService.updateUser(user);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 联系人列表（供联合贷款人等场景选择，排除自己；仅农户/买家/专家）
     * @param role 可选，限定角色（如 farmer），不传则返回农户/买家/专家全部
     */
    @GetMapping("/contacts")
    public Result<List<User>> contacts(@RequestHeader("Authorization") String token,
                                       @RequestParam(required = false) String role) {
        String userName = userService.getUserByUserNameFromToken(token);
        return Result.success(userService.listContacts(userName, role));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestHeader("Authorization") String token,
                                        @RequestBody Map<String, String> request) {
        try {
            String userName = userService.getUserByUserNameFromToken(token);
            User user = userService.getUserByUserName(userName);
            if (user == null) {
                return Result.error("用户不存在");
            }

            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return Result.error("请输入旧密码和新密码");
            }
            if (newPassword.length() < 6) {
                return Result.error("新密码长度不能少于6位");
            }

            userService.updatePassword(user, oldPassword, newPassword);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 提交角色升级申请（农户/买家 → 专家/银行）
     */
    @PostMapping("/apply-role")
    public Result<String> applyRole(@RequestHeader("Authorization") String token,
                                     @RequestBody RoleApplicationRequest request) {
        try {
            String userName = userService.getUserByUserNameFromToken(token);
            roleApplicationService.apply(userName, request);
            return Result.success("申请已提交，请等待管理员审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查看我的角色申请记录
     */
    @GetMapping("/my/applications")
    public Result<List<RoleApplication>> myApplications(@RequestHeader("Authorization") String token) {
        String userName = userService.getUserByUserNameFromToken(token);
        return Result.success(roleApplicationService.listMine(userName));
    }
}
