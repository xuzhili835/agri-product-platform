package com.agri.platform.service.impl;

import com.agri.platform.entity.Message;
import com.agri.platform.mapper.MessageMapper;
import com.agri.platform.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void send(String userName, String category, String title, String content, String linkUrl) {
        // 接收人或标题为空时静默跳过，避免业务因通知写入失败而中断
        if (userName == null || userName.isEmpty() || title == null || title.isEmpty()) {
            return;
        }
        Message message = new Message();
        message.setUserName(userName);
        message.setCategory(category);
        message.setTitle(title);
        message.setContent(content);
        message.setLinkUrl(linkUrl);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    @Override
    public Page<Message> listMine(String userName, int page, int pageSize, Integer isRead) {
        Page<Message> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserName, userName);
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        wrapper.orderByDesc(Message::getCreateTime);
        return messageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public int countUnread(String userName) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getUserName, userName).eq(Message::getIsRead, 0);
        Long count = messageMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void markRead(Integer id, String userName) {
        Message message = messageMapper.selectById(id);
        if (message == null || !userName.equals(message.getUserName())) {
            return; // 不存在或不属于本人，静默忽略
        }
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return;
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    @Override
    public void markAllRead(String userName) {
        LambdaUpdateWrapper<Message> update = new LambdaUpdateWrapper<>();
        update.eq(Message::getUserName, userName).eq(Message::getIsRead, 0).set(Message::getIsRead, 1);
        messageMapper.update(null, update);
    }

    @Override
    public void delete(Integer id, String userName) {
        Message message = messageMapper.selectById(id);
        if (message == null || !userName.equals(message.getUserName())) {
            return;
        }
        messageMapper.deleteById(id);
    }
}
