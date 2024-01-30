package org.hexastacks.heroesdesk.kotlin.test

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.*

object HeroesDeskTestUtils {

    fun createTitleOrThrow(title: String) = Title(title).getOrElse { throw AssertionError() }

    fun createDescriptionOrThrow(description: String) = Description(description).getOrElse { throw AssertionError() }

    fun createUserNameOrThrow(name: String) = UserName(name).getOrElse { throw AssertionError() }

    fun createHeroIdOrThrow(id: String) = HeroId(id).getOrElse { throw AssertionError() }

    fun createHeroOrThrow(id: String) = Hero(createUserNameOrThrow(id), createHeroIdOrThrow(id))

    fun createPendingTaskIdOrThrow(squadKey: String, id: String) =
        PendingTaskId(createSquadKeyOrThrow(squadKey), id).getOrElse { throw AssertionError() }

    fun createInProgressTaskIdOrThrow(squadKey: String, id: String): InProgressTaskId =
        InProgressTaskId(createSquadKeyOrThrow(squadKey), id).getOrElse { throw AssertionError() }

    fun HeroesDesk.getTaskOrThrow(id: TaskId): Task<*> = this.getTask(id).getOrElse { throw AssertionError() }
    fun createAdminIdOrThrow(adminId: String): AdminId = AdminId(adminId).getOrElse { throw AssertionError() }
    fun createAdminOrThrow(adminId: String): Admin =
        Admin(createAdminIdOrThrow(adminId), createUserNameOrThrow(adminId))

    fun createNameOrThrow(name: String): Name = Name(name).getOrElse { throw AssertionError() }

    fun createSquadKeyOrThrow(squadKey: String): SquadKey = SquadKey(squadKey).getOrElse { throw AssertionError() }

}