package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class PendingTaskTest : AbstractTaskTest<PendingTaskId, PendingTask>() {
    override fun createTaskOrThrow(
        id: PendingTaskId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ) =
        PendingTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(scope: ScopeKey, taskId: String): PendingTaskId =
        PendingTaskId(scope, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}