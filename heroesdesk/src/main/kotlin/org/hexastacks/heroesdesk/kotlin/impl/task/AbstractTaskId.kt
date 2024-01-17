package org.hexastacks.heroesdesk.kotlin.impl.task

abstract class AbstractTaskId() : TaskId {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractTaskId) return false

        if (scope != other.scope) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scope.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String =
        "${javaClass.simpleName}(scope=$scope, value='$value')"

}
