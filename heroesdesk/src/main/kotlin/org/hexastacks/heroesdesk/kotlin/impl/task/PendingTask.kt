package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

data class PendingTask(
    override val scope: Scope,
    override val taskId: PendingTaskId,
    override val title: Title,
    override val description: Description = Description.EMPTY_DESCRIPTION,
    override val assignees: Heroes = Heroes.EMPTY_HEROES
) : Task<PendingTaskId> {
}