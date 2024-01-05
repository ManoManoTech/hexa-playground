package org.hexastacks.heroesdesk.kotlin.impl


import org.hexastacks.heroesdesk.kotlin.impl.Description.DescriptionError

class DescriptionTest : AbstractStringValueTest<Description, DescriptionError>() {

    override val minLength = Description.MIN_LENGTH
    override val maxLength: Int = Description.MAX_LENGTH

    override fun createStringValue(value: String) = Description(value)

}