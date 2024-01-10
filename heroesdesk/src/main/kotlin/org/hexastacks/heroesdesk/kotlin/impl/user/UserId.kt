package org.hexastacks.heroesdesk.kotlin.impl.user

import org.hexastacks.heroesdesk.kotlin.impl.StringValue

sealed interface UserId : StringValue {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36
    }
}
