package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.EitherNel
import org.hexastacks.heroesdesk.kotlin.misc.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.mission.Title
import org.hexastacks.heroesdesk.kotlin.mission.Title.TitleError

class TitleTest : AbstractStringValueTest<Title, TitleError>() {
    override fun createStringValue(value: String): EitherNel<TitleError, Title> = Title(value)

    override val minLength: Int = Title.MIN_LENGTH
    override val maxLength: Int = Title.MAX_LENGTH

}