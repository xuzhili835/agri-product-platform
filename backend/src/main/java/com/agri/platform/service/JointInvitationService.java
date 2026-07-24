package com.agri.platform.service;

import com.agri.platform.entity.JointInvitation;

import java.util.List;

/**
 * 联合贷款人邀请服务：农户选联系人发起邀请 → 对方确认 → 回填资料。
 */
public interface JointInvitationService {

    /**
     * 发起邀请：创建一条「待处理」邀请，并给被邀请人发站内通知。
     *
     * @param applicantUserName 申请人（农户）userName
     * @param financeId         关联的融资申请ID
     * @param jointUserName     被邀请的联合贷款人 userName
     * @param slot              联合人位置（1 或 2）
     */
    void invite(String applicantUserName, Integer financeId, String jointUserName, int slot);

    /** 我（作为被邀请人）收到的全部邀请，带展示字段回填 */
    List<JointInvitation> listMine(String jointUserName);

    /** 某笔融资申请下的全部邀请（农户在「我的融资」详情查看各联合人确认状态） */
    List<JointInvitation> listByFinance(Integer financeId);

    /** 同意邀请：回填联合人资料到融资申请的 combination 字段，并通知申请人 */
    void accept(Integer id, String jointUserName);

    /** 拒绝邀请：通知申请人 */
    void decline(Integer id, String jointUserName);
}
