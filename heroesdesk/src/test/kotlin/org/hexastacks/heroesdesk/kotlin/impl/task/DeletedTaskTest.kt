package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class DeletedTaskTest : AbstractTaskTest<DeletedTaskId, DeletedTask>() {
    override fun createTaskOrThrow(
        id: DeletedTaskId,
        title: Title,
        description: Description,
        creator: Hero,
        assignees: Heroes
    ) =
        DeletedTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(taskId: String): DeletedTaskId =
        DeletedTaskId(taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}