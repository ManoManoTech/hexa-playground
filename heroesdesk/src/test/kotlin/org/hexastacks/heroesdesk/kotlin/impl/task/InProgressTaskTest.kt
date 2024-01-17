package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

class InProgressTaskTest : AbstractTaskTest<InProgressTaskId, InProgressTask>() {
    override fun createTaskOrThrow(
        scope: Scope,
        id: InProgressTaskId,
        title: Title,
        description: Description,
        creator: Hero,
        assignees: Heroes
    ) =
        InProgressTask(scope, id, title, description, assignees)

    override fun createTaskIdOrThrow(scope: Scope, taskId: String): InProgressTaskId =
        InProgressTaskId(scope, taskId).getOrElse { throw RuntimeException("$taskId should be valid") }

}