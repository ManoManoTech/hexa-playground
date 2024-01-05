package org.hexastacks.heroesdesk.kotlin.impl

import arrow.core.EitherNel
import arrow.core.getOrElse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

abstract class AbstractStringValueTest<T : StringValue, E : StringValueError> {

    @Test
    fun `string at min length creates stringValue`() {
        val desc = "f".repeat(minLength)

        val stringValue = createStringValue(desc)

        assertTrue(stringValue.isRight())
        assertEquals(desc, stringValue.getOrElse { throw IllegalStateException() }.value)
    }

    @Test
    fun `string above min length creates stringValue`() {
        val desc = "f".repeat(minLength + 1)

        val stringValue = createStringValue(desc)

        assertTrue(stringValue.isRight())
        assertEquals(desc, stringValue.getOrElse { throw IllegalStateException() }.value)
    }

    @Test
    fun `string below max length creates stringValue`() {
        val desc = "f".repeat(maxLength - 1)

        val description = createStringValue(desc)

        assertTrue(description.isRight())
        assertEquals(desc, description.getOrElse { throw IllegalStateException() }.value)
    }

    @Test
    fun `string at max length creates stringValue`() {
        val desc = "f".repeat(maxLength)

        val description = createStringValue(desc)

        assertTrue(description.isRight())
        assertEquals(desc, description.getOrElse { throw IllegalStateException() }.value)
    }

    @Test
    fun `stringValue not possible with string above max length`() {
        val tooLongValue = "f".repeat(maxLength + 1)

        val description = createStringValue(tooLongValue)

        assertTrue(description.isLeft())
    }

    @Test
    fun `stringValue not possible with string below min length`() {
        if (minLength != 0) {
            val tooSmallValue = "f".repeat(minLength - 1)

            val stringValue = createStringValue(tooSmallValue)

            assertTrue(stringValue.isLeft())
        }

    }

    @Test
    fun `toString displays the string value`() {
        val value = "my own desc"
        val description = stringValueOrThrow(value)

        val toString = description.toString()

        assertTrue(toString.contains(value))
    }

    @Test
    fun `two string values with the same value are equal`() {
        val value = "my own desc"
        val description1 = stringValueOrThrow(value)
        val description2 = stringValueOrThrow(value)

        assertEquals(description1, description2)
    }

    @Test
    fun `two string values with different values aren't equal`() {
        val description1 = stringValueOrThrow("description1")
        val description2 = stringValueOrThrow("description2")

        assertNotEquals(description1, description2)
    }

    @Test
    fun `two string values with the same value have the same hashcode`() {
        val value = "my own text"
        val description1 = stringValueOrThrow(value)
        val description2 = stringValueOrThrow(value)

        assertEquals(description1.hashCode(), description2.hashCode())
    }

    abstract fun createStringValue(value: String): EitherNel<E, T>
    abstract val minLength: Int
    abstract val maxLength: Int
    private fun stringValueOrThrow(value: String) = createStringValue(value).getOrElse { throw IllegalStateException() }

}
