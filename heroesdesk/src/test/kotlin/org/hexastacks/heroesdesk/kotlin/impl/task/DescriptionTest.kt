package org.hexastacks.heroesdesk.kotlin.impl.task


import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest
import org.hexastacks.heroesdesk.kotlin.impl.task.Description.DescriptionError

class DescriptionTest : AbstractStringValueTest<Description, DescriptionError>() {

    override val minLength = Description.MIN_LENGTH
    override val maxLength: Int = Description.MAX_LENGTH

    override fun createStringValue(value: String) = Description(value)

}