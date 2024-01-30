package org.hexastacks.heroesdesk.kotlin.test

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.*
import org.hexastacks.heroesdesk.kotlin.user.*

object HeroesDeskTestUtils {

    fun createTitleOrThrow(title: String) = Title(title).getOrElse { throw AssertionError() }

    fun createDescriptionOrThrow(description: String) = Description(description).getOrElse { throw AssertionError() }

    fun createUserNameOrThrow(name: String) = UserName(name).getOrElse { throw AssertionError() }

    fun createHeroIdOrThrow(id: String) = HeroId(id).getOrElse { throw AssertionError() }

    fun createHeroOrThrow(id: String) = Hero(createUserNameOrThrow(id), createHeroIdOrThrow(id))

    fun createPendingMissionIdOrThrow(squadKey: String, id: String) =
        PendingMissionId(createSquadKeyOrThrow(squadKey), id).getOrElse { throw AssertionError() }

    fun createInProgressMissionIdOrThrow(squadKey: String, id: String): InProgressMissionId =
        InProgressMissionId(createSquadKeyOrThrow(squadKey), id).getOrElse { throw AssertionError() }

    fun HeroesDesk.getMissionOrThrow(id: MissionId): Mission<*> = this.getMission(id).getOrElse { throw AssertionError() }
    fun createAdminIdOrThrow(adminId: String): AdminId = AdminId(adminId).getOrElse { throw AssertionError() }
    fun createAdminOrThrow(adminId: String): Admin =
        Admin(createAdminIdOrThrow(adminId), createUserNameOrThrow(adminId))

    fun createNameOrThrow(name: String): Name = Name(name).getOrElse { throw AssertionError() }

    fun createSquadKeyOrThrow(squadKey: String): SquadKey = SquadKey(squadKey).getOrElse { throw AssertionError() }

}