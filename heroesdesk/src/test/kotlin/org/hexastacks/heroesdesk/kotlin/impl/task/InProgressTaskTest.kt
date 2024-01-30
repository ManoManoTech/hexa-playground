package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class InProgressTaskTest : AbstractTaskTest<InProgressTaskId, InProgressTask>() {
    override fun createTaskOrThrow(
        id: InProgressTaskId,
        title: Title,
        description: Description,
        assignees: HeroIds
    ) =
        InProgressTask(id, title, description, assignees)

    override fun createTaskIdOrThrow(scope: ScopeKey, taskId: String): InProgressTaskId =
        InProgressTaskId(scope, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}