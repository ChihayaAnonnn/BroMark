package com.jay.domain.task.service;


import com.jay.domain.task.model.entity.TaskEntity;
import com.jay.domain.task.repository.ITaskRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author Jay
 * @date 2025/7/26 10:00
 * @description TODO
 */
@Service
public class TaskService implements ITaskService{

    @Resource
    private ITaskRepository taskRepository;

    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        return taskRepository.queryNoSendMessageTaskList();
    }

    @Override
    public void sendMessage(TaskEntity taskEntity) {
        taskRepository.sendMessage(taskEntity);
    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {
        taskRepository.updateTaskSendMessageCompleted(userId, messageId);
    }

    @Override
    public void updateTaskSendMessageFailed(String userId, String messageId) {
        taskRepository.updateTaskSendMessageFailed(userId, messageId);
    }
}
