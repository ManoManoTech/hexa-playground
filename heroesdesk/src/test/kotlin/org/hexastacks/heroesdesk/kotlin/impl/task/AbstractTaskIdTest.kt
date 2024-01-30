package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel
import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createScopeKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createScopeOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId.TaskIdError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

abstract class AbstractTaskIdTest<Id : TaskId> {
    @Test
    fun `taskId at min length creates taskId`() {
        val desc = "f".repeat(minLength)

        val taskId = createTaskIdOrThrow(desc)

        assertEquals(desc, taskId.value)
    }

    @Test
    fun `string above min length creates taskId`() {
        val desc = "f".repeat(minLength + 1)

        val taskId = createTaskIdOrThrow(desc)

        assertEquals(desc, taskId.value)
    }

    @Test
    fun `string below max length creates taskId`() {
        val desc = "f".repeat(maxLength - 1)

        val description = createTaskIdOrThrow(desc)

        assertEquals(desc, description.value)
    }

    @Test
    fun `string at max length creates taskId`() {
        val desc = "f".repeat(maxLength)

        val description = createTaskIdOrThrow(desc)

        assertEquals(desc, description.value)
    }

    @Test
    fun `taskId not possible with string above max length`() {
        val tooLongValue = "f".repeat(maxLength + 1)

        val description = createTaskId(tooLongValue)

        assertTrue(description.isLeft())
    }

    @Test
    fun `taskId not possible with string below min length`() {
        val tooSmallValue = "f".repeat(minLength - 1)

        val taskId = createTaskId(tooSmallValue)

        assertTrue(taskId.isLeft())
    }

    @Test
    fun `toString displays the string value`() {
        val value = "my own desc"
        val description = createTaskIdOrThrow(value)

        val toString = description.toString()

        assertTrue(toString.contains(value))
    }

    @Test
    fun `two string values with the same values are equal`() {
        val value = "my own desc"
        val description1 = createTaskIdOrThrow(value)
        val description2 = createTaskIdOrThrow(value)

        assertEquals(description1, description2)
    }

    @Test
    fun `two string values with different values aren't equal`() {
        val description1 = createTaskIdOrThrow("description1")
        val description2 = createTaskIdOrThrow("description2")

        assertNotEquals(description1, description2)
    }

    @Test
    fun `two string values with the same value have the same hashcode`() {
        val value = "my own text"
        val description1 = createTaskIdOrThrow(value)
        val description2 = createTaskIdOrThrow(value)

        assertEquals(description1.hashCode(), description2.hashCode())
    }

    abstract fun createTaskId(scope: ScopeKey, value: String): EitherNel<TaskIdError, Id>

    private fun createTaskId(value: String) =
        createTaskId(createScopeKeyOrThrow("randomScope"), value)

    private fun createTaskIdOrThrow(value: String) =
        createTaskId(createScopeKeyOrThrow("randomScope"), value).getOrElse { throw IllegalStateException() }

    private val minLength: Int = TaskId.MIN_LENGTH
    private val maxLength: Int = TaskId.MAX_LENGTH
}