package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.Description
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.Title

class DoneTaskTest : AbstractTaskTest<DoneTaskId, DoneTask>() {
    override fun createTaskOrThrow(id: DoneTaskId, title: Title, description: Description, creator: Hero) =
        DoneTask(id, title, description, creator)

    override fun createTaskIdOrThrow(taskId: String): DoneTaskId =
        DoneTaskId(taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}