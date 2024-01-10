package org.hexastacks.heroesdesk.kotlin.impl.user

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.impl.user.UserName.HeroNameError

class UserNameTest : AbstractStringValueTest<UserName, HeroNameError>() {
    override fun createStringValue(value: String): EitherNel<HeroNameError, UserName> = UserName(value)

    override val minLength: Int = UserName.MIN_LENGTH
    override val maxLength: Int = UserName.MAX_LENGTH

}