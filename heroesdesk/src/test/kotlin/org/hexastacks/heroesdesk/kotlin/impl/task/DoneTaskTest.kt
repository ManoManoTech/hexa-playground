package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

class DoneTaskTest : AbstractTaskTest<DoneTaskId, DoneTask>() {
    override fun createTaskOrThrow(
        id: DoneTaskId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ): DoneTask =
        DoneTask(id, title, description)


    override fun createTaskIdOrThrow(squadKey: SquadKey, taskId: String): DoneTaskId =
        DoneTaskId(squadKey, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}