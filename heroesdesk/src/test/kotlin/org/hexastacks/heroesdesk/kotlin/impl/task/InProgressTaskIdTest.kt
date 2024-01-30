package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey

class InProgressTaskIdTest : AbstractTaskIdTest<InProgressTaskId>() {

    override fun createTaskId(scope: ScopeKey, value: String): EitherNel<TaskId.TaskIdError, InProgressTaskId> = InProgressTaskId(scope, value)

}