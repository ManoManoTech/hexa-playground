package org.hexastacks.heroesdesk.kotlin.misc.mission

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.misc.TestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.misc.TestUtils.createSquadKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.misc.TestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.Mission
import org.hexastacks.heroesdesk.kotlin.mission.MissionId
import org.hexastacks.heroesdesk.kotlin.mission.Title
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractMissionTest<Id : MissionId, T : Mission<Id>> {

    @Test
    fun `updateTitle should return a new mission with the updated title`() {
        val mission = createMissionOrThrow("squadKey", "missionId", "title", "description")
        val newTitle = Title("new title").getOrElse { throw RuntimeException("new title should be valid") }

        val updatedMission = mission.updateTitle(newTitle)

        assertEquals(newTitle, updatedMission.title)
    }

    private fun createMissionOrThrow(
        squadKey: String,
        missionId: String,
        title: String,
        description: String
    ): T {
        val squad = createSquadKeyOrThrow(squadKey)
        return createMissionOrThrow(
            createMissionIdOrThrow(squad, missionId),
            createTitleOrThrow(title),
            createDescriptionOrThrow(description)
        )
    }

    abstract fun createMissionIdOrThrow(squad: SquadKey, missionId: String): Id

    abstract fun createMissionOrThrow(
        id: Id,
        title: Title,
        description: Description,
        assignees: HeroIds = HeroIds.empty
    ): T
}
