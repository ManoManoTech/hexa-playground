package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds

sealed interface Task<T : TaskId> {
    fun updateTitle(title: Title): Task<out TaskId> = when (this) {
        is PendingTask -> copy(title = title)
        is InProgressTask -> copy(title = title)
        is DoneTask -> copy(title = title)
    }

    fun updateDescription(description: Description): Task<out TaskId> = when (this) {
        is PendingTask -> copy(description = description)
        is InProgressTask -> copy(description = description)
        is DoneTask -> copy(description = description)
    }

    fun assign(assignees: HeroIds): Task<out TaskId> = when (this) {
        is PendingTask -> copy(assignees = assignees)
        is InProgressTask -> copy(assignees = assignees)
        is DoneTask -> this
    }

    val taskId: T
    val title: Title
    val description: Description
    val assignees: HeroIds

    fun squadKey(): SquadKey = taskId.squadKey
}