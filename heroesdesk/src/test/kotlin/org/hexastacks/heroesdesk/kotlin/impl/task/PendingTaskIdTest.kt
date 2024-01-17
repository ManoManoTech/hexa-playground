package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope

class PendingTaskIdTest : AbstractTaskIdTest<PendingTaskId>() {
    override fun createTaskId(scope: Scope, value: String): EitherNel<TaskId.TaskIdError, PendingTaskId> =
        PendingTaskId(scope, value)


}