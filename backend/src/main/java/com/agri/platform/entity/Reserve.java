package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_reserve")
public class Reserve {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String expertName;
    private String expertRealName;
    private String questioner;
    private String questionerRealName;
    private String phone;
    private String address;
    private String area;
    private String plantName;
    private String soilCondition;
    private String plantCondition;
    private String plantDetail;
    private String message;
    private String preferredTime;
    private String answer;
    private Integer status; // 0待处理 1已完成 2已拒绝 3已过期（超期自动关闭）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}