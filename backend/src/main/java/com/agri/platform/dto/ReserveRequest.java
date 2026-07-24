package com.agri.platform.dto;

import lombok.Data;

@Data
public class ReserveRequest {
    // 兼容旧字段
    private String expertId;
    private String time;
    private String reason;

    // 新字段对应数据库表 tb_reserve
    private String expertName;    // 专家姓名
    private String phone;          // 电话
    private String address;        // 地址
    private String area;           // 面积
    private String plantName;      // 农作物
    private String soilCondition;  // 土壤条件
    private String plantCondition;  // 作物条件
    private String plantDetail;     // 作物详情
    private String message;        // 留言
    private String preferredTime;  // 期望时间段（简单版：如"工作日上午""下个月"）
}
