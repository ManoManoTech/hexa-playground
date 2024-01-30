package org.hexastacks.heroesdesk.kotlin.misc.user

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.misc.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.HeroId.HeroIdError
import org.hexastacks.heroesdesk.kotlin.user.UserId

class HeroIdTest : AbstractStringValueTest<HeroId, HeroIdError>() {
    override fun createStringValue(value: String): EitherNel<HeroIdError, HeroId> = HeroId(value)

    override val minLength: Int = UserId.MIN_LENGTH
    override val maxLength: Int = UserId.MAX_LENGTH
}