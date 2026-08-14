package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_agent_message")
public class AgentMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String sessionId;
    private String userName;
    private String direction;   // user / assistant
    private String content;
    private String toolEvent;
    private LocalDateTime createTime;
}
