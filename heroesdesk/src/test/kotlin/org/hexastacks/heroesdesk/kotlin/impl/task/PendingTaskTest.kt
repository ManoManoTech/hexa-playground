package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

class PendingTaskTest : AbstractTaskTest<PendingTaskId, PendingTask>() {
    override fun createTaskOrThrow(
        id: PendingTaskId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ) =
        PendingTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(squadKey: SquadKey, taskId: String): PendingTaskId =
        PendingTaskId(squadKey, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}