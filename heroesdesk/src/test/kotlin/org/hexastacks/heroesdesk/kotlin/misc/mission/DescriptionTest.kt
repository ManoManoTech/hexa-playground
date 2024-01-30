package org.hexastacks.heroesdesk.kotlin.misc.mission


import org.hexastacks.heroesdesk.kotlin.misc.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.Description.DescriptionError

class DescriptionTest : AbstractStringValueTest<Description, DescriptionError>() {

    override val minLength = Description.MIN_LENGTH
    override val maxLength: Int = Description.MAX_LENGTH

    override fun createStringValue(value: String) = Description(value)

}