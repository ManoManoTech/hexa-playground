package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

data class InProgressTask(
    override val scope: Scope,
    override val taskId: InProgressTaskId,
    override val title: Title,
    override val description: Description,
    override val assignees: Heroes
) : Task<InProgressTaskId> {

}