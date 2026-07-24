package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站内消息实体（对应 tb_message，由 DatabaseMigrationRunner 启动时自动建表）。
 * 用于订单/预约/融资/问答等状态变更时给相关用户推送通知，供顶部铃铛与各角色数据概览展示。
 */
@Data
@TableName("tb_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 接收人 userName */
    private String userName;

    /** 分类：order/reserve/finance/question/system 等，便于前端图标与筛选 */
    private String category;

    /** 标题 */
    private String title;

    /** 正文 */
    private String content;

    /** 点击跳转路径（可选），如 /order/123 */
    private String linkUrl;

    /** 0 未读 1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
