package com.xigeandwillian.parkingsystem.client.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xigeandwillian.parkingsystem.client.service.MessageService;
import com.xigeandwillian.parkingsystem.common.entity.Message;
import com.xigeandwillian.parkingsystem.common.mapper.MessageMapper;
import com.xigeandwillian.parkingsystem.common.result.Result;
import com.xigeandwillian.parkingsystem.common.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public Result readOne(Long id) {
        Long userId = UserHolder.get();
        log.info("标记消息已读: userId={}, messageId={}", userId, id);
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .set(Message::getIsRead, 1)
                .eq(Message::getId, id)
                .eq(Message::getUserId, userId));
        return Result.ok();
    }

    @Override
    public Result readAll() {
        Long userId = UserHolder.get();
        log.info("全部标记已读: userId={}", userId);
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .set(Message::getIsRead, 1)
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
        return Result.ok();
    }
}
