package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey

class PendingTaskIdTest : AbstractTaskIdTest<PendingTaskId>() {
    override fun createTaskId(squadKey: SquadKey, value: String): EitherNel<TaskId.TaskIdError, PendingTaskId> =
        PendingTaskId(squadKey, value)

}