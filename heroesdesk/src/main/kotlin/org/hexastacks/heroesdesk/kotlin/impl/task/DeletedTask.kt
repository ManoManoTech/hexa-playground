package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

data class DeletedTask(
    override val taskId: DeletedTaskId,
    override val title: Title,
    override val description: Description,
    override val assignees: Heroes,
    override val scope: Scope
) : Task<DeletedTaskId> {

}