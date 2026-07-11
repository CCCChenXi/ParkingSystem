package com.xigeandwillian.parkingsystem.client.controller;

import com.xigeandwillian.parkingsystem.client.service.MessageService;
import com.xigeandwillian.parkingsystem.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PutMapping("/{id}/read")
    public Result readOne(@PathVariable Long id) {
        log.info("标记消息已读: {}", id);
        return messageService.readOne(id);
    }

    @PutMapping("/read-all")
    public Result readAll() {
        log.info("全部标记已读");
        return messageService.readAll();
    }
}
