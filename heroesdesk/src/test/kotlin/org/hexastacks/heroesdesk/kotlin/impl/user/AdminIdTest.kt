package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.impl.user.AdminId.AdminIdError

class AdminIdTest : AbstractStringValueTest<AdminId, AdminIdError>() {
    override fun createStringValue(value: String): EitherNel<AdminIdError, AdminId> = AdminId(value)

    override val minLength: Int = AdminId.MIN_LENGTH
    override val maxLength: Int = AdminId.MAX_LENGTH
}