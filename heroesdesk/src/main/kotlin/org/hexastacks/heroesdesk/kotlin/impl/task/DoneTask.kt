package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class DoneTask(
    override val taskId: DoneTaskId,
    override val title: Title,
    override val description: Description,
) : Task<DoneTaskId> {
    override val assignees: HeroIds = HeroIds.empty
}
