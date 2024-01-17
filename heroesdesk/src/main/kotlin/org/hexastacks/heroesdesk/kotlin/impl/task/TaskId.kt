package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.StringValue
import org.hexastacks.heroesdesk.kotlin.impl.StringValueError
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope

sealed interface TaskId : StringValue {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36
    }

    val scope: Scope

    sealed interface TaskIdError : StringValueError
    data class BelowMinLengthError(val string: String) : TaskIdError {
        override val message: String = "Task id must be above $MIN_LENGTH characters, got ${string.length} in $string"
    }

    data class AboveMaxLengthError(val string: String) : TaskIdError {
        override val message: String = "Task id must be below $MAX_LENGTH characters, got ${string.length} in $string"
    }
}