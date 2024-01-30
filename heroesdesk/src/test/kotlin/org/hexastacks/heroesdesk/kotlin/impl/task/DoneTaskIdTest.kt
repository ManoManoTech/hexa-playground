package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId.TaskIdError

class DoneTaskIdTest : AbstractTaskIdTest<DoneTaskId>() {
    override fun createTaskId(squadKey: SquadKey, value: String): EitherNel<TaskIdError, DoneTaskId> =
        DoneTaskId(squadKey, value)

}