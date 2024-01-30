package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey

class PendingTaskIdTest : AbstractTaskIdTest<PendingTaskId>() {
    override fun createTaskId(scope: ScopeKey, value: String): EitherNel<TaskId.TaskIdError, PendingTaskId> =
        PendingTaskId(scope, value)

}