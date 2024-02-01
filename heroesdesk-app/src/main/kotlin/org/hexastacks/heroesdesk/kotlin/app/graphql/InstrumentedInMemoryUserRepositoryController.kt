package org.hexastacks.heroesdesk.kotlin.app.graphql

import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InstrumentedInMemoryUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class InstrumentedInMemoryUserRepositoryController {

    @Autowired
    private lateinit var instrumentedInMemoryUserRepository: InstrumentedInMemoryUserRepository

    @MutationMapping
    fun addUser(@Argument id: String, @Argument kind: UserKind): GqlUser {
        when (kind) {
            UserKind.ADMIN -> instrumentedInMemoryUserRepository.ensureAdminExistingOrThrow(id)
            UserKind.HERO -> instrumentedInMemoryUserRepository.ensureHeroExistingOrThrow(id)
        }
        return GqlUser(id, kind.name)
    }
}

data class GqlUser(val id: String, val kind: String)

enum class UserKind {
    ADMIN, HERO
}
