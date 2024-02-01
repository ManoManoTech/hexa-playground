package org.hexastacks.heroesdesk.kotlin.app.graphql

import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphQlTest : AbstractHeroesDeskTest() {

    @LocalServerPort
    private var port: Int? = null

    private var graphQlAdapter: HeroesDeskGraphQlAdapter? = null

    override fun createHeroesDesk(): HeroesDesk {
        graphQlAdapter = HeroesDeskGraphQlAdapter("http://localhost:$port/graphql")
        return   graphQlAdapter!!
    }

    override fun ensureAdminExistingOrThrow(id: String): AdminId = graphQlAdapter!!.ensureAdminExistingOrThrow(id)

    override fun ensureHeroExistingOrThrow(id: String): HeroId = graphQlAdapter!!.ensureHeroExistingOrThrow(id)

}