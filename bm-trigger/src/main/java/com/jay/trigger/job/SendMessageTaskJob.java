package com.jay.trigger.job;

import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.jay.domain.task.model.entity.TaskEntity;
import com.jay.domain.task.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Jay
 * @date 2025/7/26 01:54
 * @description 发送MQ消息任务队列
 */
@Slf4j
@Component
public class SendMessageTaskJob {

    @Resource
    private ITaskService taskService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private IDBRouterStrategy dbRouter;


    @Scheduled(cron = "0/5 * * * * ?")
    public void exec() {
        try {
            // 获取分库数量
            int dbCount = dbRouter.dbCount();
            for (int idx = 1; idx <= dbCount; idx++) {
                int finalIdx = idx;
                threadPoolExecutor.execute(() -> {
                    try {
                        dbRouter.setDBKey(finalIdx);
                        dbRouter.setTBKey(0);

                        List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
                        if (taskEntities.isEmpty()) return;

                        // 发送MQ消息
                        for (TaskEntity taskEntity : taskEntities) {
                            // 开启线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略。
                            threadPoolExecutor.execute(() -> {
                                try{
                                    taskService.sendMessage(taskEntity);
                                    taskService.updateTaskSendMessageCompleted(taskEntity.getUserId(), taskEntity.getMessageId());
                                }catch (Exception e){
                                    log.error("定时任务，扫描MQ任务表发送消息失败 userId:{} topic:{}", taskEntity.getUserId(), taskEntity.getMessageId());
                                    taskService.updateTaskSendMessageFailed(taskEntity.getUserId(), taskEntity.getMessageId());
                                }
                            });
                        }
                    } finally {
                        dbRouter.clear();
                    }
                });
            }
        } catch (Exception e) {
            log.error("定时任务，扫描MQ任务表发送消息失败", e);
        }
    }
}
