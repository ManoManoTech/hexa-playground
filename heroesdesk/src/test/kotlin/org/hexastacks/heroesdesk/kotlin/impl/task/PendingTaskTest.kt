package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class PendingTaskTest : AbstractTaskTest<PendingTaskId, PendingTask>() {
    override fun createTaskOrThrow(
        id: PendingTaskId,
        title: Title,
        description: Description,
        creator: Hero,
        assignees: Heroes
    ) =
        PendingTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(taskId: String): PendingTaskId =
        PendingTaskId(taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}