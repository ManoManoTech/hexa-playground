package org.hexastacks.heroesdesk.kotlin.impl.user

abstract class AbstractUser<T : UserId>() : User<T> {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractUser<*>) return false

        if (id != other.id) return false

        return true
    }

    final override fun hashCode(): Int {
        return id.hashCode()
    }
}