package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

data class InProgressTask(
    override val taskId: InProgressTaskId,
    override val title: Title,
    override val description: Description,
    override val assignees: HeroIds
) : Task<InProgressTaskId>