package org.hexastacks.heroesdesk.kotlin

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.*
import org.hexastacks.heroesdesk.kotlin.ports.InstrumentedUserRepository

object HeroesDeskTestUtils {

    fun createTitleOrThrow(title: String) = Title(title).getOrElse { throw AssertionError() }

    fun createDescriptionOrThrow(description: String) = Description(description).getOrElse { throw AssertionError() }

    fun createUserNameOrThrow(name: String) = UserName(name).getOrElse { throw AssertionError() }

    fun createHeroIdOrThrow(id: String) = HeroId(id).getOrElse { throw AssertionError() }

    fun createHeroOrThrow(id: String) = Hero(createUserNameOrThrow(id), createHeroIdOrThrow(id))

    fun createPendingTaskIdOrThrow(scopeKey: String, id: String) = PendingTaskId(Scope(createNameOrThrow(scopeKey), createScopeKeyOrThrow(scopeKey)), id).getOrElse { throw AssertionError() }

    fun createInProgressTaskIdOrThrow(scopeKey: String, id: String): InProgressTaskId = InProgressTaskId(Scope(createNameOrThrow(scopeKey), createScopeKeyOrThrow(scopeKey)), id).getOrElse { throw AssertionError() }

    fun HeroesDesk.getTaskOrThrow(id: TaskId): Task<*> = this.getTask(id).getOrElse { throw AssertionError() }
    fun createAdminIdOrThrow(adminId: String): AdminId = AdminId(adminId).getOrElse { throw AssertionError() }
    fun createAdminOrThrow(adminId: String): Admin =
        Admin(createAdminIdOrThrow(adminId), createUserNameOrThrow(adminId))

    fun createNameOrThrow(name: String): Name = Name(name).getOrElse { throw AssertionError() }

    fun createScopeKeyOrThrow(scopeKey: String): ScopeKey = ScopeKey(scopeKey).getOrElse { throw AssertionError() }

}