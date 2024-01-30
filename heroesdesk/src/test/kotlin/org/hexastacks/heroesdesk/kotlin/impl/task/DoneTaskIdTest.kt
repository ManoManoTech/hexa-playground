package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId.TaskIdError

class DoneTaskIdTest : AbstractTaskIdTest<DoneTaskId>() {
    override fun createTaskId(scope: ScopeKey, value: String): EitherNel<TaskIdError, DoneTaskId> =
        DoneTaskId(scope, value)

}