package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

class InProgressTaskTest : AbstractTaskTest<InProgressTaskId, InProgressTask>() {
    override fun createTaskOrThrow(
        id: InProgressTaskId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ) =
        InProgressTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(squadKey: SquadKey, taskId: String): InProgressTaskId =
        InProgressTaskId(squadKey, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}