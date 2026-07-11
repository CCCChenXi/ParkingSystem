package com.xigeandwillian.parkingsystem.client.vo.message;

import lombok.Data;

import java.util.List;

@Data
public class SyncMessageVO {
    private String type = "sync";
    private List<MessageVO> messages;
    private Long lastTimestamp;
    private Long lastId;
}
