package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.Description
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.Title

class DeletedTaskTest : AbstractTaskTest<DeletedTaskId, DeletedTask>() {
    override fun createTaskOrThrow(id: DeletedTaskId, title: Title, description: Description, creator: Hero) =
        DeletedTask(id, title, description, creator)

    override fun createTaskIdOrThrow(taskId: String): DeletedTaskId =
        DeletedTaskId(taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}