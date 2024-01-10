package org.hexastacks.heroesdesk.kotlin.impl.scope

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name.NameError

class NameTest : AbstractStringValueTest<Name, NameError>() {
    override fun createStringValue(value: String): EitherNel<NameError, Name> = Name(value)

    override val minLength: Int = Name.MIN_LENGTH
    override val maxLength: Int = Name.MAX_LENGTH

}