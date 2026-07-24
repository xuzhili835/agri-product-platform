package com.agri.platform.service.impl;

import com.agri.platform.dto.BankFinanceStats;
import com.agri.platform.dto.FinanceApprovalRequest;
import com.agri.platform.dto.FinanceProductRequest;
import com.agri.platform.dto.FinanceRequest;
import com.agri.platform.entity.Finance;
import com.agri.platform.entity.FinanceProduct;
import com.agri.platform.entity.Product;
import com.agri.platform.entity.PurchaseDetail;
import com.agri.platform.entity.User;
import com.agri.platform.mapper.FinanceMapper;
import com.agri.platform.mapper.FinanceProductMapper;
import com.agri.platform.mapper.ProductMapper;
import com.agri.platform.mapper.PurchaseDetailMapper;
import com.agri.platform.mapper.UserMapper;
import com.agri.platform.service.FinanceService;
import com.agri.platform.service.JointInvitationService;
import com.agri.platform.service.MessageService;
import com.agri.platform.service.RepaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinanceServiceImpl implements FinanceService {

    @Autowired
    private FinanceProductMapper productMapper;

    @Autowired
    private FinanceMapper financeMapper;

    /** 农产品(tb_product) Mapper，用于智能匹配中按 realName↔ownName 关联农户在售/种植的农产品 */
    @Autowired
    private ProductMapper agriProductMapper;

    /** 订单明细(tb_purchase_detail) Mapper，用于智能匹配中统计农户交易活跃度 */
    @Autowired
    private PurchaseDetailMapper purchaseDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private JointInvitationService jointInvitationService;

    @Autowired
    private RepaymentService repaymentService;

    @Override
    public void publishProduct(String userName, FinanceProductRequest request) {
        // 验证用户是银行角色（按 role 字段判断，不能用用户名前缀判断）
        User user = userMapper.selectById(userName);
        if (user == null || !"bank".equals(user.getRole())) {
            throw new RuntimeException("只有银行可以发布融资产品");
        }

        FinanceProduct product = new FinanceProduct();
        // 产品名称由银行自行命名（如"助农贷"）；银行名称固定取自该银行注册信息(realName)，
        // 禁止前端冒名填写其它银行，从根本上杜绝"青岛银行冒名发布中国储蓄银行产品"。
        product.setProductName(request.getProductName());
        product.setBankName(user.getRealName());
        // 记录发布银行账号，用于银行间数据隔离（银行只能管理本行产品）
        product.setBankUserName(userName);
        product.setIntroduce(request.getIntroduce());
        product.setBankPhone(request.getBankPhone());
        product.setMoney(request.getMoney());
        product.setRate(request.getRate());
        product.setRepayment(request.getRepayment());
        product.setStatus(0); // 在售/可申请
        productMapper.insert(product);
    }

    @Override
    public Page<FinanceProduct> getProductList(int page, int pageSize) {
        Page<FinanceProduct> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<FinanceProduct> wrapper = new LambdaQueryWrapper<>();
        // 公开列表仅展示「在售」产品：暂停供应的产品农户不可再申请，故不对公众展示
        wrapper.eq(FinanceProduct::getStatus, 0);
        wrapper.orderByDesc(FinanceProduct::getCreateTime);
        return productMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Page<FinanceProduct> getBankProductList(String bankUserName, int page, int pageSize) {
        Page<FinanceProduct> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<FinanceProduct> wrapper = new LambdaQueryWrapper<>();
        // 银行数据隔离：仅返回本行发布的产品（含已暂停），便于银行查看与恢复供应
        wrapper.eq(FinanceProduct::getBankUserName, bankUserName);
        wrapper.orderByDesc(FinanceProduct::getCreateTime);
        return productMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public FinanceProduct getProductById(Integer productId) {
        return productMapper.selectById(productId);
    }

    @Override
    @Transactional
    public void applyFinance(String userName, FinanceRequest request) {
        // 验证用户是农户角色（按 role 字段判断，不能用用户名前缀判断）
        User user = userMapper.selectById(userName);
        if (user == null || !"farmer".equals(user.getRole())) {
            throw new RuntimeException("只有农户可以申请融资");
        }

        // 申请必须使用该账号「注册时」的真实姓名和手机号：以后端查到的用户信息为准，
        // 忽略前端传入，避免被篡改或填错
        String realName = user.getRealName();
        String phone = user.getPhone();
        if (realName == null || realName.trim().isEmpty()) {
            throw new RuntimeException("请先在个人中心完善真实姓名后再申请融资");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("请先在个人中心完善手机号后再申请融资");
        }

        // 获取融资产品信息
        FinanceProduct product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("融资产品不存在");
        }
        // 暂停供应的产品不可继续申请（status: 0在售 1暂停供应；历史数据 status 为 null 视为在售）
        if (product.getStatus() != null && product.getStatus() == 1) {
            throw new RuntimeException("该融资产品已暂停供应，暂不可申请");
        }

        // 校验申请金额（money 为数据库 NOT NULL 列，必须校验，避免直接抛出原始 SQL 异常）
        if (request.getMoney() == null || request.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("请输入正确的申请金额");
        }

        // 申请原因 / 还款来源：去首尾空格后不少于 15 个字
        String purpose = request.getPurpose() == null ? "" : request.getPurpose().trim();
        if (purpose.length() < 15) {
            throw new RuntimeException("申请原因不能少于15个字");
        }
        String repaymentSource = request.getRepaymentSource() == null ? "" : request.getRepaymentSource().trim();
        if (repaymentSource.length() < 15) {
            throw new RuntimeException("还款来源不能少于15个字");
        }

        Finance finance = new Finance();
        finance.setProductId(request.getProductId());
        // 冗余写入所属银行账号，用于银行工作台按行隔离展示/审批（与 realName 冗余惯例一致）
        finance.setBankUserName(product.getBankUserName());
        finance.setOwnName(userName);
        finance.setRealName(realName.trim());    // 使用注册真实姓名
        finance.setPhone(phone.trim());          // 使用注册手机号
        finance.setIdNum(request.getIdNum());
        finance.setMoney(request.getMoney());
        finance.setRate(product.getRate());
        finance.setRepayment(product.getRepayment());
        finance.setStatus(0); // 申请中
        finance.setCombinationName1(request.getCombinationName1());
        finance.setCombinationPhone1(request.getCombinationPhone1());
        finance.setCombinationIdnum1(request.getCombinationIdnum1());
        finance.setCombinationName2(request.getCombinationName2());
        finance.setCombinationPhone2(request.getCombinationPhone2());
        finance.setCombinationIdnum2(request.getCombinationIdnum2());
        finance.setFileInfo(request.getFileInfo());
        finance.setPurpose(purpose);
        finance.setRepaymentSource(repaymentSource);
        financeMapper.insert(finance);

        // 联合贷款人：按选中的联系人创建邀请（待对方确认）。
        // 联合人的姓名/电话/身份证号在对方同意后由 JointInvitationService 回填到 combination 字段，
        // 因此此处 combination 字段先留空。
        String j1 = request.getJointUserName1();
        String j2 = request.getJointUserName2();
        if (j1 != null && j2 != null && !j1.trim().isEmpty() && j1.trim().equals(j2.trim())) {
            throw new RuntimeException("两个联合贷款人不能是同一个人");
        }
        if (j1 != null && !j1.trim().isEmpty()) {
            jointInvitationService.invite(userName, finance.getFinanceId(), j1.trim(), 1);
        }
        if (j2 != null && !j2.trim().isEmpty()) {
            jointInvitationService.invite(userName, finance.getFinanceId(), j2.trim(), 2);
        }
    }

    @Override
    public Page<Finance> getApplyList(String userName, int page, int pageSize) {
        Page<Finance> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Finance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Finance::getOwnName, userName);
        wrapper.orderByDesc(Finance::getCreateTime);
        Page<Finance> result = financeMapper.selectPage(pageParam, wrapper);
        // 回填产品（银行）名称，便于农户在「我的融资」中看到申请的是哪个产品
        fillProductName(result.getRecords());
        return result;
    }

    @Override
    public Page<Finance> getBankApprovalList(int page, int pageSize) {
        Page<Finance> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Finance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Finance::getStatus, 0); // 待审批
        wrapper.orderByDesc(Finance::getCreateTime);
        return financeMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional
    public void approveFinance(Integer financeId, String bankUserName, FinanceApprovalRequest request) {
        Finance finance = financeMapper.selectById(financeId);
        if (finance == null) {
            throw new RuntimeException("融资申请不存在");
        }
        // 权限校验：仅本行银行可审批该申请（融资审批归属于各银行，管理员不再参与审批）。
        User operator = userMapper.selectById(bankUserName);
        if (operator == null) {
            throw new RuntimeException("用户不存在");
        }
        boolean isOwnerBank = "bank".equals(operator.getRole())
                && finance.getBankUserName() != null
                && finance.getBankUserName().equals(bankUserName);
        if (!isOwnerBank) {
            throw new RuntimeException("无权审批该融资申请（仅本行银行可审批）");
        }
        Integer previousStatus = finance.getStatus();
        finance.setStatus(request.getStatus());
        finance.setRemark(request.getRemark());
        financeMapper.updateById(finance);

        // 首次审批（从「申请中」流转）联动调整农户信用分：通过 +1（封顶5），拒绝 -1（保底1）
        Integer newStatus = request.getStatus();
        if (previousStatus != null && previousStatus == 0
                && newStatus != null && (newStatus == 1 || newStatus == 2)) {
            User farmer = userMapper.selectById(finance.getOwnName());
            if (farmer != null) {
                int c = farmer.getCredit() == null ? 5 : farmer.getCredit();
                c = newStatus == 1 ? Math.min(5, c + 1) : Math.max(1, c - 1);
                farmer.setCredit(c);
                userMapper.updateById(farmer);
            }
        }

        // 通过审批：自动生成等额本息还款计划
        if (newStatus != null && newStatus == 1) {
            repaymentService.generatePlan(finance);
        }

        // 通知农户审批结果（1通过 2拒绝）
        Integer st = request.getStatus();
        String title;
        String content;
        if (st != null && st == 1) {
            title = "融资申请已通过";
            content = "您的融资申请已审批通过。";
        } else if (st != null && st == 2) {
            title = "融资申请未通过";
            String remark = request.getRemark();
            content = "您的融资申请未通过。" + (remark == null || remark.isEmpty() ? "" : "备注：" + remark);
        } else {
            title = "融资申请状态更新";
            content = "您的融资申请状态已更新。";
        }
        messageService.send(finance.getOwnName(), "finance", title, content, "/farmer/my-finance");
    }

    @Override
    public void updateFinance(Integer financeId, String userName, FinanceRequest request) {
        // 只有申请者可以修改（在审批前）
        Finance finance = financeMapper.selectById(financeId);
        if (finance == null) {
            throw new RuntimeException("融资申请不存在");
        }
        if (!finance.getOwnName().equals(userName)) {
            throw new RuntimeException("无权限修改此申请");
        }
        if (finance.getStatus() != 0) {
            throw new RuntimeException("只有申请中的记录可以修改");
        }
        // 更新允许修改的字段
        if (request.getRealName() != null) finance.setRealName(request.getRealName());
        if (request.getPhone() != null) finance.setPhone(request.getPhone());
        if (request.getIdNum() != null) finance.setIdNum(request.getIdNum());
        if (request.getMoney() != null) finance.setMoney(request.getMoney());
        if (request.getCombinationName1() != null) finance.setCombinationName1(request.getCombinationName1());
        if (request.getCombinationPhone1() != null) finance.setCombinationPhone1(request.getCombinationPhone1());
        if (request.getCombinationIdnum1() != null) finance.setCombinationIdnum1(request.getCombinationIdnum1());
        if (request.getCombinationName2() != null) finance.setCombinationName2(request.getCombinationName2());
        if (request.getCombinationPhone2() != null) finance.setCombinationPhone2(request.getCombinationPhone2());
        if (request.getCombinationIdnum2() != null) finance.setCombinationIdnum2(request.getCombinationIdnum2());
        if (request.getFileInfo() != null) finance.setFileInfo(request.getFileInfo());
        financeMapper.updateById(finance);
    }

    @Override
    public void deleteFinance(Integer financeId, String userName) {
        Finance finance = financeMapper.selectById(financeId);
        if (finance == null) {
            throw new RuntimeException("融资申请不存在");
        }
        if (!finance.getOwnName().equals(userName)) {
            throw new RuntimeException("无权限删除此申请");
        }
        // 仅允许删除「申请中(0)」与「已拒绝(2)」的记录：申请中可撤销，被拒绝后可清理；
        // 已通过(1)的融资已进入放款/还款流程，不可删除。
        Integer st = finance.getStatus();
        if (st == null || (st != 0 && st != 2)) {
            throw new RuntimeException("只有申请中或已拒绝的记录可以删除");
        }
        financeMapper.deleteById(financeId);
    }

    @Override
    public Page<Finance> getBankApplications(String bankUserName, Integer status, String keyword, Integer productId, String startDate, String endDate, int page, int pageSize) {
        Page<Finance> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Finance> wrapper = new LambdaQueryWrapper<>();

        // 银行数据隔离：仅本行产品的融资申请
        wrapper.eq(Finance::getBankUserName, bankUserName);

        // 状态过滤（不传则返回全部状态）
        if (status != null) {
            wrapper.eq(Finance::getStatus, status);
        }

        // 融资产品过滤：按所选产品ID精确筛选（含已暂停产品对应的申请）
        if (productId != null) {
            wrapper.eq(Finance::getProductId, productId);
        }

        // 关键词：模糊匹配 真实姓名 / 手机号 / 用户名
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Finance::getRealName, kw)
                    .or().like(Finance::getPhone, kw)
                    .or().like(Finance::getOwnName, kw));
        }

        // 申请时间区间
        LocalDateTime start = parseDateStart(startDate);
        LocalDateTime end = parseDateEnd(endDate);
        if (start != null) {
            wrapper.ge(Finance::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(Finance::getCreateTime, end);
        }

        wrapper.orderByDesc(Finance::getCreateTime);
        Page<Finance> result = financeMapper.selectPage(pageParam, wrapper);
        fillProductName(result.getRecords());
        fillCredit(result.getRecords());
        return result;
    }

    @Override
    public Page<Finance> getBankMatch(String bankUserName, BigDecimal minMoney, BigDecimal maxMoney, Integer repayment, int page, int pageSize) {
        // 智能匹配：仅本行产品的「申请中」(status=0) 申请参与匹配。
        // 综合匹配度以「信用/联合贷款人信用/交易活跃度/借贷负担」等多重因素自动计算为主；
        // 金额/期限目标为可选细化指标——未填则直接按自动评分排序，填了则按 7:3 与目标契合度融合。
        LambdaQueryWrapper<Finance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Finance::getBankUserName, bankUserName).eq(Finance::getStatus, 0);
        List<Finance> all = financeMapper.selectList(wrapper);
        if (all.isEmpty()) {
            Page<Finance> empty = new Page<>(page, pageSize, 0);
            empty.setRecords(new ArrayList<>());
            return empty;
        }
        fillProductName(all);
        fillCredit(all);
        fillProductNames(all);

        // 预计算各维度数据，避免逐条查询
        Map<Integer, Integer> jointCreditMap = buildJointCreditMap(all);   // financeId -> 联合贷款人均信用(1-5)
        Map<String, Integer> orderCountMap = buildOrderCountMap(all);      // realName -> 成交订单条数
        Map<String, Integer> loanCountMap = buildActiveLoanCountMap(all);  // ownName -> 已通过贷款笔数

        boolean hasTarget = (minMoney != null && minMoney.compareTo(BigDecimal.ZERO) > 0)
                || (maxMoney != null && maxMoney.compareTo(BigDecimal.ZERO) > 0)
                || (repayment != null && repayment > 0);

        for (Finance f : all) {
            f.setMatchScore(calcMatchScore(f, jointCreditMap, orderCountMap, loanCountMap, minMoney, maxMoney, repayment, hasTarget));
        }

        // 排序：匹配度降序 → 信用分降序 → 金额降序
        all.sort((a, b) -> {
            int sa = a.getMatchScore() == null ? 0 : a.getMatchScore();
            int sb = b.getMatchScore() == null ? 0 : b.getMatchScore();
            if (sb != sa) return Integer.compare(sb, sa);
            int ca = a.getCredit() == null ? 0 : a.getCredit();
            int cb = b.getCredit() == null ? 0 : b.getCredit();
            if (cb != ca) return Integer.compare(cb, ca);
            BigDecimal ma = a.getMoney() == null ? BigDecimal.ZERO : a.getMoney();
            BigDecimal mb = b.getMoney() == null ? BigDecimal.ZERO : b.getMoney();
            return mb.compareTo(ma);
        });

        // 内存分页（评分排序需在全集中进行，否则高分申请可能落在后续页；数据量不大可一次加载）
        int total = all.size();
        int from = Math.min(Math.max(page - 1, 0) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        Page<Finance> result = new Page<>(page, pageSize, total);
        result.setRecords(all.subList(from, to));
        return result;
    }

    /**
     * 综合匹配度(0-100)：
     *   自动评分（未设目标时即为最终分）：农户信用30 + 联合贷款人信用20 + 交易活跃度25 + 借贷负担25
     *   目标契合（可选，0-100）：金额契合(0-40) + 期限契合(0-20) 归一
     * 若银行设置了任意金额/期限目标：最终分 = 自动评分*0.7 + 目标契合*0.3；否则直接取自动评分。
     */
    private int calcMatchScore(Finance f, Map<Integer, Integer> jointCreditMap, Map<String, Integer> orderCountMap,
                               Map<String, Integer> loanCountMap, BigDecimal minMoney, BigDecimal maxMoney,
                               Integer repayment, boolean hasTarget) {
        int credit = scoreCreditAuto(f.getCredit());                                   // 0-30
        int joint = scoreJointAuto(jointCreditMap.get(f.getFinanceId()));              // 0-20
        int activity = scoreActivity(orderCountMap.getOrDefault(f.getRealName(), 0));  // 0-25
        int burden = scoreBurden(loanCountMap.getOrDefault(f.getOwnName(), 0));        // 0-25
        int autoScore = clamp(credit + joint + activity + burden, 0, 100);
        if (!hasTarget) return autoScore;
        int alignment = scoreAlignment(f.getMoney(), f.getRepayment(), minMoney, maxMoney, repayment); // 0-100
        return clamp(Math.round(autoScore * 0.7f + alignment * 0.3f), 0, 100);
    }

    /** 收集每条申请的联合贷款人均信用(1-5)：combination 字段为对方真实姓名，按 realName 反查 tb_user.credit 取均值。 */
    private Map<Integer, Integer> buildJointCreditMap(List<Finance> list) {
        Map<Integer, Integer> result = new java.util.HashMap<>();
        Set<String> names = new java.util.HashSet<>();
        for (Finance f : list) {
            if (f.getCombinationName1() != null && !f.getCombinationName1().trim().isEmpty()) names.add(f.getCombinationName1().trim());
            if (f.getCombinationName2() != null && !f.getCombinationName2().trim().isEmpty()) names.add(f.getCombinationName2().trim());
        }
        Map<String, Integer> creditByName = new java.util.HashMap<>();
        if (!names.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getRealName, names));
            for (User u : users) {
                if (u.getRealName() != null) creditByName.put(u.getRealName(), u.getCredit() == null ? 5 : u.getCredit());
            }
        }
        for (Finance f : list) {
            int sum = 0, n = 0;
            for (String cn : new String[]{f.getCombinationName1(), f.getCombinationName2()}) {
                if (cn == null || cn.trim().isEmpty()) continue;
                Integer c = creditByName.get(cn.trim());
                if (c != null) { sum += c; n++; }
            }
            if (n > 0) result.put(f.getFinanceId(), Math.round((float) sum / n));
        }
        return result;
    }

    /** 统计每位农户(realName)的成交订单条数：其发布的农产品(tb_product.order_id)对应的 tb_purchase_detail 数。 */
    private Map<String, Integer> buildOrderCountMap(List<Finance> list) {
        Map<String, Integer> result = new java.util.HashMap<>();
        Set<String> realNames = list.stream()
                .map(Finance::getRealName)
                .filter(java.util.Objects::nonNull)
                .filter(n -> !n.isEmpty())
                .collect(Collectors.toSet());
        if (realNames.isEmpty()) return result;
        // realName -> 其商品 order_id 列表
        List<Product> products = agriProductMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getOwnName, realNames));
        Map<String, List<Integer>> pidsByOwner = new java.util.HashMap<>();
        Set<Integer> allPids = new java.util.HashSet<>();
        for (Product p : products) {
            if (p.getOwnName() == null || p.getOrderId() == null) continue;
            pidsByOwner.computeIfAbsent(p.getOwnName(), k -> new ArrayList<>()).add(p.getOrderId());
            allPids.add(p.getOrderId());
        }
        if (allPids.isEmpty()) return result;
        // 统计每个商品的订单明细条数（本平台 PurchaseDetail.orderId 即商品 order_id）
        List<PurchaseDetail> details = purchaseDetailMapper.selectList(
                new LambdaQueryWrapper<PurchaseDetail>().in(PurchaseDetail::getOrderId, allPids));
        Map<Integer, Long> cntByPid = details.stream()
                .filter(d -> d.getOrderId() != null)
                .collect(Collectors.groupingBy(PurchaseDetail::getOrderId, Collectors.counting()));
        for (Map.Entry<String, List<Integer>> e : pidsByOwner.entrySet()) {
            long cnt = e.getValue().stream().mapToLong(pid -> cntByPid.getOrDefault(pid, 0L)).sum();
            result.put(e.getKey(), (int) Math.min(cnt, Integer.MAX_VALUE));
        }
        return result;
    }

    /** 统计每位农户(userName)已通过的贷款笔数：已有多笔贷款视为借贷负担，匹配度相应降低。 */
    private Map<String, Integer> buildActiveLoanCountMap(List<Finance> list) {
        Map<String, Integer> result = new java.util.HashMap<>();
        Set<String> userNames = list.stream()
                .map(Finance::getOwnName)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (userNames.isEmpty()) return result;
        List<Finance> approved = financeMapper.selectList(
                new LambdaQueryWrapper<Finance>().in(Finance::getOwnName, userNames).eq(Finance::getStatus, 1));
        Map<String, Long> cnt = approved.stream()
                .collect(Collectors.groupingBy(Finance::getOwnName, Collectors.counting()));
        for (String u : userNames) result.put(u, cnt.getOrDefault(u, 0L).intValue());
        return result;
    }

    /** 农户信用(0-30)：信用分(1-5)线性映射。 */
    private int scoreCreditAuto(Integer credit) {
        int c = credit == null ? 0 : credit;
        if (c > 5) c = 5;
        if (c < 0) c = 0;
        return Math.round(c * 30f / 5f);
    }

    /** 联合贷款人均信用(0-20)：无联合人记中性10；有则按均信用(1-5)映射。 */
    private int scoreJointAuto(Integer jointCredit) {
        if (jointCredit == null) return 10;
        int c = jointCredit;
        if (c > 5) c = 5;
        if (c < 0) c = 0;
        return Math.round(c * 20f / 5f);
    }

    /** 交易活跃度(0-25)：成交订单条数越多越活跃。 */
    private int scoreActivity(int orderCount) {
        if (orderCount <= 0) return 5;
        if (orderCount <= 3) return 15;
        if (orderCount <= 10) return 22;
        return 25;
    }

    /** 借贷负担(0-25)：已通过贷款笔数越少越优（0笔满分，逐级递减）。 */
    private int scoreBurden(int loanCount) {
        switch (loanCount) {
            case 0: return 25;
            case 1: return 18;
            case 2: return 12;
            case 3: return 6;
            default: return 3;
        }
    }

    /** 目标契合度(0-100)：金额契合(0-40)与期限契合(0-20)各自归一到0-100后，按已设维度取均值。 */
    private int scoreAlignment(BigDecimal money, Integer term, BigDecimal min, BigDecimal max, Integer target) {
        boolean hasMin = min != null && min.compareTo(BigDecimal.ZERO) > 0;
        boolean hasMax = max != null && max.compareTo(BigDecimal.ZERO) > 0;
        boolean hasTerm = target != null && target > 0;
        double sum = 0;
        int dims = 0;
        if (hasMin || hasMax) {
            sum += scoreAmountFit(money, min, max) / 40.0 * 100;
            dims++;
        }
        if (hasTerm) {
            sum += scoreTermFit(term, target) / 20.0 * 100;
            dims++;
        }
        if (dims == 0) return 100;
        return (int) Math.round(sum / dims);
    }

    /** 金额契合度(0-40)：在[min,max]内满分；低于min按 money/min 衰减；高于max按 max/money 衰减；无金额目标记满分。 */
    private int scoreAmountFit(BigDecimal money, BigDecimal min, BigDecimal max) {
        boolean hasMin = min != null && min.compareTo(BigDecimal.ZERO) > 0;
        boolean hasMax = max != null && max.compareTo(BigDecimal.ZERO) > 0;
        if (!hasMin && !hasMax) return 40;
        if (money == null) return 20;
        double m = money.doubleValue();
        if (hasMin && hasMax) {
            double lo = min.doubleValue(), hi = max.doubleValue();
            if (m >= lo && m <= hi) return 40;
            if (m < lo) return (int) Math.round(40 * m / lo);
            return (int) Math.round(40 * hi / m);
        }
        if (hasMin) {
            double lo = min.doubleValue();
            return m >= lo ? 40 : (int) Math.round(40 * m / lo);
        }
        double hi = max.doubleValue();
        return m <= hi ? 40 : (int) Math.round(40 * hi / m);
    }

    /** 期限契合度(0-20)：与目标月数差距越小越高；无期限目标记满分，申请未填期限记中性10。 */
    private int scoreTermFit(Integer term, Integer target) {
        if (target == null || target <= 0) return 20;
        if (term == null) return 10;
        int diff = Math.abs(term - target);
        if (diff == 0) return 20;
        if (diff <= 3) return 16;
        if (diff <= 6) return 12;
        if (diff <= 12) return 8;
        return 4;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public BankFinanceStats getBankStats(String bankUserName) {
        // 数据量较小，一次性拉取后在内存聚合；仅统计本行产品的融资申请（银行数据隔离）
        LambdaQueryWrapper<Finance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Finance::getBankUserName, bankUserName);
        wrapper.orderByDesc(Finance::getCreateTime);
        List<Finance> all = financeMapper.selectList(wrapper);

        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal monthlyAmount = BigDecimal.ZERO;

        // 本月起止
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        List<Finance> pendingList = new ArrayList<>();

        for (Finance f : all) {
            Integer st = f.getStatus();
            BigDecimal money = f.getMoney() == null ? BigDecimal.ZERO : f.getMoney();
            if (st == null || st == 0) {
                pendingCount++;
                pendingList.add(f);
            } else if (st == 1) {
                approvedCount++;
                totalAmount = totalAmount.add(money);
                if (f.getCreateTime() != null && !f.getCreateTime().isBefore(monthStart)) {
                    monthlyAmount = monthlyAmount.add(money);
                }
            } else if (st == 2) {
                rejectedCount++;
                // 申请总额按「已通过」口径统计（不含已拒绝金额），故此处不计入 totalAmount
            }
        }

        // 待审批：取最早的 5 条（最急需处理）
        pendingList.sort((a, b) -> {
            LocalDateTime ta = a.getCreateTime();
            LocalDateTime tb = b.getCreateTime();
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });
        if (pendingList.size() > 5) {
            pendingList = new ArrayList<>(pendingList.subList(0, 5));
        }

        // 最近已处理：非申请中的最新 5 条（all 已按 createTime 倒序）
        List<Finance> recentList = all.stream()
                .filter(f -> f.getStatus() != null && f.getStatus() != 0)
                .limit(5)
                .collect(Collectors.toList());

        // 审批通过率 = 已通过 / (已通过 + 已拒绝)
        int denominator = approvedCount + rejectedCount;
        int approvalRate = denominator > 0 ? Math.round((float) approvedCount * 100 / denominator) : 0;

        fillProductName(pendingList);
        fillProductName(recentList);
        fillCredit(pendingList);
        fillCredit(recentList);

        BankFinanceStats stats = new BankFinanceStats();
        stats.setPendingCount(pendingCount);
        stats.setApprovedCount(approvedCount);
        stats.setRejectedCount(rejectedCount);
        stats.setTotalAmount(totalAmount);
        stats.setMonthlyAmount(monthlyAmount);
        stats.setApprovalRate(approvalRate);
        stats.setPendingList(pendingList);
        stats.setRecentList(recentList);
        return stats;
    }

    @Override
    public void updateProduct(String userName, Integer productId, FinanceProductRequest request) {
        requireBankRole(userName);
        FinanceProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("融资产品不存在");
        }
        requireOwnProduct(userName, product);
        // 不可随意更改：核心条款(额度/利率/期限)发布后锁定；产品名称、介绍与联系电话可随时调整
        if (request.getProductName() != null) product.setProductName(request.getProductName());
        if (request.getIntroduce() != null) product.setIntroduce(request.getIntroduce());
        if (request.getBankPhone() != null) product.setBankPhone(request.getBankPhone());
        productMapper.updateById(product);
    }

    @Override
    public void setProductStatus(String userName, Integer productId, Integer status) {
        requireBankRole(userName);
        FinanceProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("融资产品不存在");
        }
        requireOwnProduct(userName, product);
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("非法的状态参数");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    // ==================== 私有辅助 ====================

    /** 校验当前用户为银行角色 */
    private void requireBankRole(String userName) {
        User user = userMapper.selectById(userName);
        if (user == null || !"bank".equals(user.getRole())) {
            throw new RuntimeException("只有银行可以管理融资产品");
        }
    }

    /** 校验融资产品归属于当前银行，防止跨行编辑/暂停/恢复 */
    private void requireOwnProduct(String bankUserName, FinanceProduct product) {
        if (product.getBankUserName() == null || !product.getBankUserName().equals(bankUserName)) {
            throw new RuntimeException("无权操作该融资产品");
        }
    }

    /** 批量回填每条申请对应的产品（银行）名称，便于银行工作台展示 */
    private void fillProductName(List<Finance> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Integer> productIds = list.stream()
                .map(Finance::getProductId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }
        List<FinanceProduct> products = productMapper.selectBatchIds(productIds);
        Map<Integer, FinanceProduct> productMap = products.stream()
                .collect(Collectors.toMap(FinanceProduct::getProductId, Function.identity()));
        for (Finance f : list) {
            if (f.getProductId() == null) continue;
            FinanceProduct p = productMap.get(f.getProductId());
            if (p != null) {
                // 优先展示产品名称（如"助农贷"），旧数据无产品名时回退银行名
                f.setProductName(p.getProductName() != null && !p.getProductName().isEmpty()
                        ? p.getProductName() : p.getBankName());
                // 同步回填产品上下架状态：农户端可据此提示「该产品已暂停供应」
                f.setProductStatus(p.getStatus());
            }
        }
    }

    /** 批量回填每条申请对应申请人的信用分（tb_user.credit，1-5），供银行审批参考 */
    private void fillCredit(List<Finance> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<String> names = list.stream()
                .map(Finance::getOwnName)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (names.isEmpty()) {
            return;
        }
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getUserName, names));
        Map<String, Integer> creditMap = users.stream()
                .collect(Collectors.toMap(User::getUserName,
                        u -> u.getCredit() == null ? 5 : u.getCredit(),
                        (a, b) -> a));
        for (Finance f : list) {
            f.setCredit(creditMap.get(f.getOwnName()));
        }
    }

    /**
     * 批量回填每条申请对应农户在售/种植的农产品名称。
     * 关联依据：Finance.realName（农户真名）↔ Product.ownName（农产品发布方真名）。
     * 每位农户取最多 5 个去重标题，逗号(、)拼接，供银行智能匹配展示「融资申请 ↔ 农产品」。
     */
    private void fillProductNames(List<Finance> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<String> realNames = list.stream()
                .map(Finance::getRealName)
                .filter(java.util.Objects::nonNull)
                .filter(n -> !n.isEmpty())
                .collect(Collectors.toSet());
        if (realNames.isEmpty()) {
            return;
        }
        List<Product> products = agriProductMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getOwnName, realNames));
        Map<String, List<String>> titlesByOwner = products.stream()
                .filter(p -> p.getOwnName() != null && p.getTitle() != null)
                .collect(Collectors.groupingBy(Product::getOwnName,
                        Collectors.mapping(Product::getTitle, Collectors.toList())));
        for (Finance f : list) {
            List<String> titles = titlesByOwner.get(f.getRealName());
            if (titles != null && !titles.isEmpty()) {
                String joined = titles.stream().distinct().limit(5).collect(Collectors.joining("、"));
                f.setProductNames(joined);
            }
        }
    }

    /** 解析 "yyyy-MM-dd" 为当日 00:00:00，非法返回 null */
    private LocalDateTime parseDateStart(String date) {
        if (date == null || date.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(date.trim()).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析 "yyyy-MM-dd" 为当日 23:59:59，非法返回 null */
    private LocalDateTime parseDateEnd(String date) {
        if (date == null || date.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(date.trim()).atTime(23, 59, 59);
        } catch (Exception e) {
            return null;
        }
    }
}