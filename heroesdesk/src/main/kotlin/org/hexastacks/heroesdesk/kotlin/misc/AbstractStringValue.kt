package org.hexastacks.heroesdesk.kotlin.misc

open class AbstractStringValue(override val value: String) : StringValue {

    final override fun toString(): String = "${javaClass.simpleName}(value='$value')"

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractStringValue

        return value == other.value
    }

    final override fun hashCode(): Int {
        return value.hashCode()
    }
}

interface StringValueError: ValidationError