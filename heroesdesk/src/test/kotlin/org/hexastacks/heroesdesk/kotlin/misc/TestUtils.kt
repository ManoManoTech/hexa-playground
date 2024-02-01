package org.hexastacks.heroesdesk.kotlin.misc

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.Title
import org.hexastacks.heroesdesk.kotlin.user.Hero
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.UserName

object TestUtils {

    fun createSquadKeyOrThrow(squadKey: String): SquadKey =
        SquadKey(squadKey).getOrElse { throw RuntimeException("squad key should be valid: $it") }

    fun createTitleOrThrow(title: String): Title =
        Title(title).getOrElse { throw RuntimeException("title should be valid: $it") }

    fun createDescriptionOrThrow(description: String): Description =
        Description(description).getOrElse { throw RuntimeException("description should be valid: $it") }

    fun createHeroNameOrThrow(creator: String): UserName =
        UserName(creator).getOrElse { throw RuntimeException("$creator should be valid: $it") }

    fun createHeroIdOrThrow(heroId: String): HeroId =
        HeroId(heroId).getOrElse { throw RuntimeException("$heroId should be valid: $it") }

    fun createHeroOrThrow(creator: String, id: String): Hero =
        Hero(createHeroIdOrThrow(id), createHeroNameOrThrow(creator))

}