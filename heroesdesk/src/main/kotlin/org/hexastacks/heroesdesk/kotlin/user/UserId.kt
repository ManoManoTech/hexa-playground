package org.hexastacks.heroesdesk.kotlin.user

import org.hexastacks.heroesdesk.kotlin.misc.StringValue

sealed interface UserId : StringValue {

    companion object {
        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 36
    }
}
