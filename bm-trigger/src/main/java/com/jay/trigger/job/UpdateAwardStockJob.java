package com.jay.trigger.job;

import com.jay.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.jay.domain.strategy.service.IRaffleStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/7/8 20:16
 * @description 更新奖品库存任务：采用redis更新缓存库存，异步更新数据库
 */
@Slf4j
@Component
public class UpdateAwardStockJob {
    @Resource
    private IRaffleStock raffleStock;

    /**
     * 通过定时任务获取存放在 redis 中的库存消费队列。并依照队列中的数据更新数据库。<p>
     * 因为存放数据是延迟存放，消费也是定时任务，这样会对数据库的库表更新的压力会降低很多，不会产生大量的竞争。
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void execute() {
        try {
            StrategyAwardStockKeyVO strategyAwardStockKeyVO = raffleStock.takeQueueValue();
            if (strategyAwardStockKeyVO == null) {
                return;
            }

            log.info("定时任务，更新奖品库存 strategyId:{} awardId:{}",
                    strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
            raffleStock.updateStrategyAwardStock(strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());

        } catch (Exception e) {
            log.error("定时任务，更新奖品库存失败", e);
        }
    }
}
