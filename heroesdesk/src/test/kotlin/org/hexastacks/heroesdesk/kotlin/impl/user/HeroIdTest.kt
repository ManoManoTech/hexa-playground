package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroId.HeroIdError

class HeroIdTest : AbstractStringValueTest<HeroId, HeroIdError>() {
    override fun createStringValue(value: String): EitherNel<HeroIdError, HeroId> = HeroId(value)

    override val minLength: Int = UserId.MIN_LENGTH
    override val maxLength: Int = UserId.MAX_LENGTH
}