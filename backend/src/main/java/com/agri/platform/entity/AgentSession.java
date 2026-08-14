package com.agri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_agent_session")
public class AgentSession {
    @TableId(type = IdType.INPUT)   // 主键由代码生成(UUID),与 User.userName/Expert.expertName 同型,不能用全局 auto
    private String sessionId;
    private String userName;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastActiveTime;
}
