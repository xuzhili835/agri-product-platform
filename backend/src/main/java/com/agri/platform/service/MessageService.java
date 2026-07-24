package com.agri.platform.service;

import com.agri.platform.entity.Message;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 站内消息服务：各业务在状态变更时调用 send 推送通知，用户通过铃铛读取/标记已读。
 */
public interface MessageService {

    /** 发送一条站内消息（供其他 Service 调用） */
    void send(String userName, String category, String title, String content, String linkUrl);

    /** 分页查询我的消息（isRead 为空查全部） */
    Page<Message> listMine(String userName, int page, int pageSize, Integer isRead);

    /** 未读消息数（用于铃铛徽标） */
    int countUnread(String userName);

    /** 标记单条为已读（带归属校验） */
    void markRead(Integer id, String userName);

    /** 全部标记已读 */
    void markAllRead(String userName);

    /** 删除一条消息（带归属校验） */
    void delete(Integer id, String userName);
}
