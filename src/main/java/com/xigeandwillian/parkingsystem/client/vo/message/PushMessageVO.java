package com.xigeandwillian.parkingsystem.client.vo.message;

import lombok.Data;

@Data
public class PushMessageVO {
    private String type = "push";
    private MessageVO message;
    private Long timestamp;
    private Long id;
}
