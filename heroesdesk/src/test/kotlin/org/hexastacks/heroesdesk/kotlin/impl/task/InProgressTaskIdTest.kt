package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope

class InProgressTaskIdTest : AbstractTaskIdTest<InProgressTaskId>() {

    override fun createTaskId(scope: Scope, value: String): EitherNel<TaskId.TaskIdError, InProgressTaskId> = InProgressTaskId(scope, value)

}