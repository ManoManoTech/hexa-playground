package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.Description
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.Title

class PendingTaskTest : AbstractTaskTest<PendingTaskId, PendingTask>() {
    override fun createTaskOrThrow(id: PendingTaskId, title: Title, description: Description, creator: Hero) =
        PendingTask(id, title, description, creator)

    override fun createTaskIdOrThrow(taskId: String): PendingTaskId =
        PendingTaskId(taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}