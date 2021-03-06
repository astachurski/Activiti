/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.runtime.api.impl;

import java.util.Collections;

import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.impl.TaskImpl;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.api.task.runtime.events.TaskUpdatedEvent;
import org.activiti.api.task.runtime.events.listener.TaskRuntimeEventListener;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskRuntimeImplTest {

    private TaskRuntimeImpl taskRuntime;

    @Mock
    private SecurityManager securityManager;

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRuntimeEventListener<TaskUpdatedEvent> listener;

    @Before
    public void setUp() {
        initMocks(this);
        taskRuntime = spy(new TaskRuntimeImpl(taskService,
                            null,
                            securityManager,
                            null,
                            null,
                            null,
                            Collections.singletonList(listener)));
        when(securityManager.getAuthenticatedUserId()).thenReturn("user");
    }

    @Test
    public void updateShouldThrowExceptionWhenAssigneeIsNotSet() {
        //given
        UpdateTaskPayload updateTaskPayload = TaskPayloadBuilder
                .update()
                .withTaskId("taskId")
                .withDescription("new description")
                .build();
        doReturn(new TaskImpl()).when(taskRuntime).task("taskId");

        //when
        Throwable throwable = catchThrowable(() -> taskRuntime.update(updateTaskPayload));

        //then
        assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You cannot update a task where you are not the assignee");
    }

    @Test
    public void updateShouldBeAbleToUpdateDescriptionOnly() {
        //given
        UpdateTaskPayload updateTaskPayload = TaskPayloadBuilder
                .update()
                .withTaskId("taskId")
                .withDescription("new description")
                .build();
        TaskImpl task = new TaskImpl();
        task.setAssignee("user");
        doReturn(task).when(taskRuntime).task("taskId");

        TaskQuery taskQuery = mock(TaskQuery.class);
        given(taskQuery.taskId("taskId")).willReturn(taskQuery);
        given(taskService.createTaskQuery()).willReturn(taskQuery);

        Task internalTask = mock(Task.class);
        given(taskQuery.singleResult()).willReturn(internalTask);

        //when
        org.activiti.api.task.model.Task updatedTask = taskRuntime.update(updateTaskPayload);

        //then
        verify(internalTask).setDescription("new description");
        verifyNoMoreInteractions(internalTask);
        ArgumentCaptor<TaskUpdatedEvent> captor = ArgumentCaptor.forClass(TaskUpdatedEvent.class);
        verify(listener).onEvent(captor.capture());
        assertThat(captor.getValue().getEntity()).isEqualTo(updatedTask);
    }
}