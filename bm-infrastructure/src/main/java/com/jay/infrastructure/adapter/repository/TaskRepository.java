package com.jay.infrastructure.adapter.repository;


import cn.hutool.core.bean.BeanUtil;
import com.jay.domain.task.model.entity.TaskEntity;
import com.jay.domain.task.repository.ITaskRepository;
import com.jay.infrastructure.dao.ITaskDao;
import com.jay.infrastructure.dao.po.Task;
import com.jay.infrastructure.event.EventPublisher;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jay
 * @date 2025/7/26 10:04
 * @description TODO
 */
@Repository
public class TaskRepository implements ITaskRepository {

    @Resource
    private ITaskDao taskDao;

    @Resource
    private EventPublisher eventPublisher;

    @Override
    public void sendMessage(TaskEntity taskEntity) {
        eventPublisher.publish(taskEntity.getTopic(), taskEntity.getMessage());
    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {
        Task taskReq = new Task();
        taskReq.setUserId(userId);
        taskReq.setMessageId(messageId);
        taskDao.updateTaskSendMessageCompleted(taskReq);
    }

    @Override
    public void updateTaskSendMessageFailed(String userId, String messageId) {
        Task taskReq = new Task();
        taskReq.setUserId(userId);
        taskReq.setMessageId(messageId);
        taskDao.updateTaskSendMessageFailed(taskReq);
    }

    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        List<Task> tasks = taskDao.queryNoSendMessageTaskList();

        return tasks.stream()
                .map(task -> BeanUtil.copyProperties(task, TaskEntity.class))
                .collect(Collectors.toList());
    }
}
