package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_financing_intention")
public class FinancingIntention {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String userName;
    private String realName;
    private String phone;
    private String address;
    private Integer amount; // 期望金额(万)
    private String application;
    private String item; // 农作物
    private Integer area; // 种植面积(亩)
    private Integer repaymentPeriod;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}