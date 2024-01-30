package org.hexastacks.heroesdesk.kotlin.misc.user

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.misc.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.user.UserName
import org.hexastacks.heroesdesk.kotlin.user.UserName.HeroNameError

class UserNameTest : AbstractStringValueTest<UserName, HeroNameError>() {
    override fun createStringValue(value: String): EitherNel<HeroNameError, UserName> = UserName(value)

    override val minLength: Int = UserName.MIN_LENGTH
    override val maxLength: Int = UserName.MAX_LENGTH

}