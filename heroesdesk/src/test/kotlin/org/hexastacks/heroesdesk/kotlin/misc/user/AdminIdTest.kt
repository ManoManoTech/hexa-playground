package org.hexastacks.heroesdesk.kotlin.misc.user

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.misc.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.AdminId.AdminIdError

class AdminIdTest : AbstractStringValueTest<AdminId, AdminIdError>() {
    override fun createStringValue(value: String): EitherNel<AdminIdError, AdminId> = AdminId(value)

    override val minLength: Int = AdminId.MIN_LENGTH
    override val maxLength: Int = AdminId.MAX_LENGTH
}