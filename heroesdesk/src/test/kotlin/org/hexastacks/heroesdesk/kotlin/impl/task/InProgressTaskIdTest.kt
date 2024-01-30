package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey

class InProgressTaskIdTest : AbstractTaskIdTest<InProgressTaskId>() {

    override fun createTaskId(squadKey: SquadKey, value: String): EitherNel<TaskId.TaskIdError, InProgressTaskId> = InProgressTaskId(squadKey, value)

}