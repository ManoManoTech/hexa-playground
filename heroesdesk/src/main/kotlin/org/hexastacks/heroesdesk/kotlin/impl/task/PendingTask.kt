package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.Description
import org.hexastacks.heroesdesk.kotlin.impl.Hero
import org.hexastacks.heroesdesk.kotlin.impl.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.Title

data class PendingTask(
    override val taskId: PendingTaskId,
    override val title: Title,
    override val description: Description = Description.EMPTY_DESCRIPTION,
    override val creator: Hero,
    override val assignees: Heroes = Heroes.EMPTY_HEROES
) : Task<PendingTaskId> {

}