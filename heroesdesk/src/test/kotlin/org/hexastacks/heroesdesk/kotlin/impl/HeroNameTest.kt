package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.HeroName.HeroNameError

class HeroNameTest : AbstractStringValueTest<HeroName, HeroNameError>() {
    override fun createStringValue(value: String): EitherNel<HeroNameError, HeroName> = HeroName(value)

    override val minLength: Int = HeroName.MIN_LENGTH
    override val maxLength: Int = HeroName.MAX_LENGTH

}