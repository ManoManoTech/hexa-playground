package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class PendingTask(
    override val taskId: PendingTaskId,
    override val title: Title,
    override val description: Description = Description.EMPTY_DESCRIPTION,
    override val assignees: HeroIds = HeroIds.empty
) : Task<PendingTaskId>