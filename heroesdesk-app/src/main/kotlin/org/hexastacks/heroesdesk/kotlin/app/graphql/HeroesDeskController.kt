package org.hexastacks.heroesdesk.kotlin.app.graphql

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.misc.ErrorMessage
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class HeroesDeskController {


    @Autowired
    private lateinit var heroesDesk: HeroesDesk

    @MutationMapping
    fun createSquad(@Argument squadKey: String, @Argument name: String, @Argument creator: String): GqlSquad =
        either {
            val sqKey = SquadKey(squadKey).bind()
            val sqName = Name(name).bind()
            val sqCreator = AdminId(creator).bind()
            heroesDesk.createSquad(sqKey, sqName, sqCreator).bind()
        }.map {
            GqlSquad(it.key.value, it.name.value)
        }.getOrElse {
            throw HeroesDeskControllerException(it)
        }

    @MutationMapping
    fun assignSquad(
        @Argument squadKey: String,
        @Argument assignees: List<String>,
        @Argument changeAuthor: String
    ): GlResult {
        // TODO heroesDesk.assignSquad()
        return GlResult.SUCCESS
    }
}

enum class GlResult {
    SUCCESS,
    FAILURE
}

class HeroesDeskControllerException(val errorMessages: NonEmptyList<ErrorMessage>) : RuntimeException()
data class GqlSquad(val squadKey: String, val name: String)