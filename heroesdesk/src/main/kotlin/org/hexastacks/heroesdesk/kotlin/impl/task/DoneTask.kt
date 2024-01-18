package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

data class DoneTask(
    override val scope: Scope,
    override val taskId: DoneTaskId,
    override val title: Title,
    override val description: Description,
) : Task<DoneTaskId> {
    override val assignees: Heroes = Heroes.empty
}
