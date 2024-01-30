package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class DoneTaskTest : AbstractTaskTest<DoneTaskId, DoneTask>() {
    override fun createTaskOrThrow(
        id: DoneTaskId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ): DoneTask =
        DoneTask(id, title, description)


    override fun createTaskIdOrThrow(scope: ScopeKey, taskId: String): DoneTaskId =
        DoneTaskId(scope, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}