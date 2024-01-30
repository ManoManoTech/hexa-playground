package org.hexastacks.heroesdesk.kotlin.impl.squad

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Name.NameError

class NameTest : AbstractStringValueTest<Name, NameError>() {
    override fun createStringValue(value: String): EitherNel<NameError, Name> = Name(value)

    override val minLength: Int = Name.MIN_LENGTH
    override val maxLength: Int = Name.MAX_LENGTH

}