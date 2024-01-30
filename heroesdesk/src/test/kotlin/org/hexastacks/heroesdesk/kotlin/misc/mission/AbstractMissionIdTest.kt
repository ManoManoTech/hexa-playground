package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.EitherNel
import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.misc.TestUtils.createSquadKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.mission.MissionId
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.MissionId.MissionIdError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

abstract class AbstractMissionIdTest<Id : MissionId> {
    @Test
    fun `missionId at min length creates missionId`() {
        val desc = "f".repeat(minLength)

        val missionId = createMissionIdOrThrow(desc)

        assertEquals(desc, missionId.value)
    }

    @Test
    fun `string above min length creates missionId`() {
        val desc = "f".repeat(minLength + 1)

        val missionId = createMissionIdOrThrow(desc)

        assertEquals(desc, missionId.value)
    }

    @Test
    fun `string below max length creates missionId`() {
        val desc = "f".repeat(maxLength - 1)

        val description = createMissionIdOrThrow(desc)

        assertEquals(desc, description.value)
    }

    @Test
    fun `string at max length creates missionId`() {
        val desc = "f".repeat(maxLength)

        val description = createMissionIdOrThrow(desc)

        assertEquals(desc, description.value)
    }

    @Test
    fun `missionId not possible with string above max length`() {
        val tooLongValue = "f".repeat(maxLength + 1)

        val description = createMissionId(tooLongValue)

        assertTrue(description.isLeft())
    }

    @Test
    fun `missionId not possible with string below min length`() {
        val tooSmallValue = "f".repeat(minLength - 1)

        val missionId = createMissionId(tooSmallValue)

        assertTrue(missionId.isLeft())
    }

    @Test
    fun `toString displays the string value`() {
        val value = "my own desc"
        val description = createMissionIdOrThrow(value)

        val toString = description.toString()

        assertTrue(toString.contains(value))
    }

    @Test
    fun `two string values with the same values are equal`() {
        val value = "my own desc"
        val description1 = createMissionIdOrThrow(value)
        val description2 = createMissionIdOrThrow(value)

        assertEquals(description1, description2)
    }

    @Test
    fun `two string values with different values aren't equal`() {
        val description1 = createMissionIdOrThrow("description1")
        val description2 = createMissionIdOrThrow("description2")

        assertNotEquals(description1, description2)
    }

    @Test
    fun `two string values with the same value have the same hashcode`() {
        val value = "my own text"
        val description1 = createMissionIdOrThrow(value)
        val description2 = createMissionIdOrThrow(value)

        assertEquals(description1.hashCode(), description2.hashCode())
    }

    abstract fun createMissionId(squad: SquadKey, value: String): EitherNel<MissionIdError, Id>

    private fun createMissionId(value: String) =
        createMissionId(createSquadKeyOrThrow("randomSquad"), value)

    private fun createMissionIdOrThrow(value: String) =
        createMissionId(createSquadKeyOrThrow("randomSquad"), value).getOrElse { throw IllegalStateException() }

    private val minLength: Int = MissionId.MIN_LENGTH
    private val maxLength: Int = MissionId.MAX_LENGTH
}