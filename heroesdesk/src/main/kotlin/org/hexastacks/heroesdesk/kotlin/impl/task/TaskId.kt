package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.ErrorMessage
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope

sealed interface TaskId {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36
    }

    val scope: Scope
    val value: String

    sealed interface TaskIdError : ErrorMessage

    data class BelowMinLengthError(val string: String) : TaskIdError {
        override val message: String = "Task id must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : TaskIdError {
        override val message: String = "Task id must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}