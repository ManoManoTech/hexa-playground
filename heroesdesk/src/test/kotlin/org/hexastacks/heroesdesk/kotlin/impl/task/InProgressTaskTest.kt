package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class InProgressTaskTest : AbstractTaskTest<InProgressTaskId, InProgressTask>() {
    override fun createTaskOrThrow(
        id: InProgressTaskId,
        title: Title,
        description: Description,
        creator: Hero,
        assignees: Heroes
    ) =
        InProgressTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(taskId: String): InProgressTaskId =
        InProgressTaskId(taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}