package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.HeroId.HeroIdError

class HeroIdTest : AbstractStringValueTest<HeroId, HeroIdError>() {
    override fun createStringValue(value: String): EitherNel<HeroIdError, HeroId> = HeroId(value)

    override val minLength: Int = HeroId.MIN_LENGTH
    override val maxLength: Int = HeroId.MAX_LENGTH
}