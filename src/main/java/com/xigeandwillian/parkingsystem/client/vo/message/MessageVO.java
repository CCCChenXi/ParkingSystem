package com.xigeandwillian.parkingsystem.client.vo.message;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String title;
    private String content;
    private Integer type;
    private Integer isRead;
    private LocalDateTime createTime;
}
