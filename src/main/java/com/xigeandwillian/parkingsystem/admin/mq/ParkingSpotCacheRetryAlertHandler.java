package com.xigeandwillian.parkingsystem.admin.mq;

import com.xigeandwillian.parkingsystem.common.constant.MQConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ParkingSpotCacheRetryAlertHandler {

    @RabbitListener(queues = MQConstant.PARKING_SPOT_CACHE_RETRY_ALERT_QUEUE)
    public void handle(ParkingSpotCacheRetryEvent event) {
        log.error("========== 人工介入告警 ==========");
        log.error("缓存清理彻底失败，lotId={}，已重试{}次，请检查Redis状态", event.getLotId(), event.getRetryCount());
        log.error("==================================");
    }
}
