package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

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

    fun assign(assignees: Heroes): Task<out TaskId> = when (this) {
        is PendingTask -> copy(assignees = assignees)
        is InProgressTask -> copy(assignees = assignees)
        is DoneTask -> this
    }

    val scope: Scope
    val taskId: T
    val title: Title
    val description: Description
    val assignees: Heroes
}