package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.*

sealed interface Task<T : TaskId> {
    fun updateTitle(title: Title): Task<out TaskId> = when (this) {
        is PendingTask -> copy(title = title)
        is InProgressTask -> copy(title = title)
        is DoneTask -> copy(title = title)
        is DeletedTask -> copy(title = title)
    }

    fun updateDescription(description: Description): Task<out TaskId> = when (this) {
        is PendingTask -> copy(description = description)
        is InProgressTask -> copy(description = description)
        is DoneTask -> copy(description = description)
        is DeletedTask -> copy(description = description)
    }

    fun assign(assignees: Heroes): Task<out TaskId> = when (this) {
        is PendingTask -> copy(assignees = assignees)
        is InProgressTask -> copy(assignees = assignees)
        is DoneTask -> copy(assignees = assignees)
        is DeletedTask -> copy(assignees = assignees)
    }

    val taskId: T
    val title: Title
    val description: Description
    val creator: Hero
    val assignees: Heroes
}