package com.agri.platform.controller;

import com.agri.platform.common.Result;
import com.agri.platform.dto.AdminExpertRequest;
import com.agri.platform.dto.AdminUserRequest;
import com.agri.platform.entity.Expert;
import com.agri.platform.entity.Finance;
import com.agri.platform.entity.Product;
import com.agri.platform.entity.RoleApplication;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.*;
import com.agri.platform.service.RoleApplicationService;
import com.agri.platform.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private FinanceMapper financeMapper;

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ReserveMapper reserveMapper;

    @Autowired
    private ExpertMapper expertMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RoleApplicationService roleApplicationService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 商品数量
        stats.put("productCount", productMapper.selectCount(null));

        // 融资申请数量
        stats.put("financeCount", financeMapper.selectCount(null));

        // 知识文章数量
        stats.put("knowledgeCount", knowledgeMapper.selectCount(null));

        // 问题数量
        stats.put("questionCount", questionMapper.selectCount(null));

        // 预约数量
        stats.put("reserveCount", reserveMapper.selectCount(null));

        return Result.success(stats);
    }

    // ================= 用户管理 =================

    @GetMapping("/users")
    public Result<List<User>> getAllUsers(@RequestHeader("Authorization") String token,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String keyword) {
        requireAdmin(token);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUserName, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getPhone, kw));
        }
        wrapper.orderByDesc(User::getCreateTime);
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /** 新增用户（任意角色；expert 同时建立专家资料） */
    @PostMapping("/user")
    public Result<String> createUser(@RequestHeader("Authorization") String token,
                                      @RequestBody AdminUserRequest request) {
        requireAdmin(token);
        if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
            return Result.error("请填写用户名");
        }
        if (userMapper.selectById(request.getUserName()) != null) {
            return Result.error("用户名已存在");
        }
        String role = request.getRole();
        if (role == null || !role.matches("farmer|buyer|expert|bank|admin")) {
            return Result.error("角色类型错误");
        }

        User user = new User();
        user.setUserName(request.getUserName());
        String pwd = (request.getPassword() == null || request.getPassword().isEmpty()) ? "123456" : request.getPassword();
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdentityNum(request.getIdentityNum());
        user.setAddress(request.getAddress());
        user.setRole(role);
        user.setIntegral(500);
        user.setCredit(5);
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        if ("expert".equals(role)) {
            Expert expert = new Expert();
            expert.setUserName(request.getUserName());
            expert.setRealName(request.getRealName());
            expert.setPhone(request.getPhone());
            expert.setProfession(request.getProfession());
            expert.setPosition(request.getPosition());
            expert.setBelong(request.getBelong());
            expertMapper.insert(expert);
        }
        return Result.success("新增成功");
    }

    /** 编辑用户（可改资料/角色/状态；角色变化时同步专家资料） */
    @PutMapping("/user/{userName}")
    public Result<String> updateUser(@RequestHeader("Authorization") String token,
                                      @PathVariable String userName,
                                      @RequestBody AdminUserRequest request) {
        requireAdmin(token);
        User user = userMapper.selectById(userName);
        if (user == null) {
            return Result.error("用户不存在");
        }
        String oldRole = user.getRole();

        // 真实姓名不可修改（它作为 tb_product.own_name 等表的"发布方"快照被冗余存储，改名会造孤儿数据）
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getIdentityNum() != null) user.setIdentityNum(request.getIdentityNum());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        // 管理员可调整农户信用分（0-5），影响其融资智能匹配与展示
        if (request.getCredit() != null && request.getCredit() >= 0 && request.getCredit() <= 5) {
            user.setCredit(request.getCredit());
        }
        if (request.getRole() != null && request.getRole().matches("farmer|buyer|expert|bank|admin")) {
            user.setRole(request.getRole());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 角色变化时同步专家资料
        String newRole = user.getRole();
        if ("expert".equals(newRole) && expertMapper.selectById(userName) == null) {
            Expert expert = new Expert();
            expert.setUserName(userName);
            expert.setRealName(user.getRealName());
            expert.setPhone(user.getPhone());
            expert.setProfession(request.getProfession());
            expert.setPosition(request.getPosition());
            expert.setBelong(request.getBelong());
            expertMapper.insert(expert);
        } else if (!"expert".equals(newRole) && "expert".equals(oldRole)) {
            expertMapper.deleteById(userName);
        }
        return Result.success("修改成功");
    }

    /** 启用/禁用用户 */
    @PutMapping("/user/{userName}/status")
    public Result<String> updateUserStatus(@RequestHeader("Authorization") String token,
                                            @PathVariable String userName,
                                            @RequestBody Map<String, Object> body) {
        User admin = requireAdmin(token);
        User user = userMapper.selectById(userName);
        if (user == null) {
            return Result.error("用户不存在");
        }
        Object st = body.get("status");
        int newStatus = (st != null && Integer.valueOf(1).equals(Integer.valueOf(st.toString()))) ? 1 : 0;

        // 禁用保护：不能禁用当前登录的管理员；不能禁用最后一个启用的管理员
        if (newStatus == 0) {
            if (admin.getUserName().equals(userName)) {
                return Result.error("不能禁用当前登录的管理员账号");
            }
            if ("admin".equals(user.getRole())) {
                Long activeAdmins = userMapper.selectCount(
                        new LambdaQueryWrapper<User>().eq(User::getRole, "admin").eq(User::getStatus, 1));
                if (activeAdmins != null && activeAdmins <= 1) {
                    return Result.error("系统至少需保留一个启用的管理员账号");
                }
            }
        }
        user.setStatus(newStatus);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success("操作成功");
    }

    /** 删除用户（专家则一并删除专家资料；不可删除自己和最后一个管理员） */
    @DeleteMapping("/user/{userName}")
    public Result<String> deleteUser(@RequestHeader("Authorization") String token,
                                      @PathVariable String userName) {
        User admin = requireAdmin(token);
        if (admin.getUserName().equals(userName)) {
            return Result.error("不能删除当前登录的管理员账号");
        }
        User user = userMapper.selectById(userName);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            Long adminCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));
            if (adminCount != null && adminCount <= 1) {
                return Result.error("系统至少需保留一个管理员账号");
            }
        }
        if ("expert".equals(user.getRole())) {
            expertMapper.deleteById(userName);
        }
        userMapper.deleteById(userName);
        return Result.success("删除成功");
    }

    // ================= 角色申请审核 =================

    @GetMapping("/applications")
    public Result<List<RoleApplication>> listApplications(@RequestHeader("Authorization") String token,
                                                           @RequestParam(required = false) Integer status) {
        requireAdmin(token);
        return Result.success(roleApplicationService.listAll(status));
    }

    @PutMapping("/application/{id}")
    public Result<String> reviewApplication(@RequestHeader("Authorization") String token,
                                             @PathVariable Integer id,
                                             @RequestBody Map<String, Object> body) {
        User admin = requireAdmin(token);
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        String remark = body.get("reviewRemark") != null ? body.get("reviewRemark").toString() : null;
        roleApplicationService.review(id, admin.getUserName(), status, remark);
        return Result.success("审核完成");
    }

    // ================= 专家管理 =================

    /** 校验当前登录用户是否为管理员 */
    private User requireAdmin(String token) {
        String userName = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        User user = userMapper.selectById(userName);
        if (user == null || !"admin".equals(user.getRole())) {
            throw new RuntimeException("无权限，仅管理员可操作");
        }
        return user;
    }

    /** 专家列表 */
    @GetMapping("/experts")
    public Result<List<Expert>> listExperts(@RequestHeader("Authorization") String token) {
        requireAdmin(token);
        return Result.success(expertMapper.selectList(null));
    }

    /** 新增专家（同时创建 role=expert 的用户与 tb_expert 记录） */
    @PostMapping("/expert")
    public Result<String> createExpert(@RequestHeader("Authorization") String token,
                                        @RequestBody AdminExpertRequest request) {
        requireAdmin(token);
        if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
            return Result.error("请填写用户名");
        }
        if (userMapper.selectById(request.getUserName()) != null) {
            return Result.error("用户名已存在");
        }

        // 创建用户账号
        User user = new User();
        user.setUserName(request.getUserName());
        String pwd = (request.getPassword() == null || request.getPassword().isEmpty()) ? "123456" : request.getPassword();
        user.setPassword(passwordEncoder.encode(pwd));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole("expert");
        user.setIntegral(500);
        user.setCredit(5);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 创建专家资料
        Expert expert = new Expert();
        expert.setUserName(request.getUserName());
        expert.setRealName(request.getRealName());
        expert.setPhone(request.getPhone());
        expert.setProfession(request.getProfession());
        expert.setPosition(request.getPosition());
        expert.setBelong(request.getBelong());
        expertMapper.insert(expert);

        return Result.success("新增成功");
    }

    /** 修改专家资料 */
    @PutMapping("/expert/{userName}")
    public Result<String> updateExpert(@RequestHeader("Authorization") String token,
                                        @PathVariable String userName,
                                        @RequestBody AdminExpertRequest request) {
        requireAdmin(token);
        Expert expert = expertMapper.selectById(userName);
        if (expert == null) {
            return Result.error("专家不存在");
        }
        if (request.getRealName() != null) expert.setRealName(request.getRealName());
        if (request.getPhone() != null) expert.setPhone(request.getPhone());
        if (request.getProfession() != null) expert.setProfession(request.getProfession());
        if (request.getPosition() != null) expert.setPosition(request.getPosition());
        if (request.getBelong() != null) expert.setBelong(request.getBelong());
        expertMapper.updateById(expert);

        // 同步用户表的 realName/phone
        User user = userMapper.selectById(userName);
        if (user != null) {
            if (request.getRealName() != null) user.setRealName(request.getRealName());
            if (request.getPhone() != null) user.setPhone(request.getPhone());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
        return Result.success("修改成功");
    }

    /** 删除专家（同时删除专家资料与用户账号） */
    @DeleteMapping("/expert/{userName}")
    public Result<String> deleteExpert(@RequestHeader("Authorization") String token,
                                        @PathVariable String userName) {
        requireAdmin(token);
        expertMapper.deleteById(userName);
        // 删除对应用号（避免遗留无专家资料的 expert 角色用户）
        User user = userMapper.selectById(userName);
        if (user != null && "expert".equals(user.getRole())) {
            userMapper.deleteById(userName);
        }
        return Result.success("删除成功");
    }

    // ================= 商品管理 =================

    /** 商品列表（可选关键字，按标题模糊） */
    @GetMapping("/products")
    public Result<List<Product>> listProducts(@RequestHeader("Authorization") String token,
                                               @RequestParam(required = false) String keyword) {
        requireAdmin(token);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Product::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return Result.success(productMapper.selectList(wrapper));
    }

    /** 管理员修改商品（不限归属） */
    @PutMapping("/product/{orderId}")
    public Result<String> adminUpdateProduct(@RequestHeader("Authorization") String token,
                                              @PathVariable Integer orderId,
                                              @RequestBody Map<String, Object> body) {
        requireAdmin(token);
        Product p = productMapper.selectById(orderId);
        if (p == null) {
            return Result.error("商品不存在");
        }
        if (body.containsKey("title")) p.setTitle((String) body.get("title"));
        if (body.containsKey("content")) p.setContent((String) body.get("content"));
        if (body.get("price") != null) p.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.containsKey("picPath")) p.setPicPath((String) body.get("picPath"));
        if (body.containsKey("type")) p.setType((String) body.get("type"));
        if (body.get("orderStatus") != null) p.setOrderStatus(Integer.valueOf(body.get("orderStatus").toString()));
        p.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(p);
        return Result.success("修改成功");
    }

    /** 管理员删除商品（不限归属） */
    @DeleteMapping("/product/{orderId}")
    public Result<String> adminDeleteProduct(@RequestHeader("Authorization") String token,
                                              @PathVariable Integer orderId) {
        requireAdmin(token);
        productMapper.deleteById(orderId);
        return Result.success("删除成功");
    }

    // ================= 融资管理 =================

    /** 融资申请列表（可选状态过滤：0申请中 1已通过 2已驳回） */
    @GetMapping("/finances")
    public Result<List<Finance>> listFinances(@RequestHeader("Authorization") String token,
                                               @RequestParam(required = false) Integer status) {
        requireAdmin(token);
        LambdaQueryWrapper<Finance> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Finance::getStatus, status);
        }
        wrapper.orderByDesc(Finance::getCreateTime);
        return Result.success(financeMapper.selectList(wrapper));
    }
}
