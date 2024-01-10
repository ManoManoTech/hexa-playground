package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

data class DoneTask(
    override val taskId: DoneTaskId,
    override val title: Title,
    override val description: Description,
    override val assignees: Heroes
) : Task<DoneTaskId> {

}
