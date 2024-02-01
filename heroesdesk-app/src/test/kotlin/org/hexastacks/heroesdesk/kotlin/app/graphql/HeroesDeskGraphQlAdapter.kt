package org.hexastacks.heroesdesk.kotlin.app.graphql

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.EitherNel
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import kotlinx.serialization.json.*
import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.app.graphql.ExceptionResolver.Companion.GRAPHQL_ERROR_TYPE
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.mission.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.user.AdminId
import org.hexastacks.heroesdesk.kotlin.user.HeroId
import org.hexastacks.heroesdesk.kotlin.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.user.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class HeroesDeskGraphQlAdapter(private val uri: String) : HeroesDesk {

    private var logger: Logger = LoggerFactory.getLogger(HeroesDeskGraphQlAdapter::class.java)

    private val client: HttpClient = HttpClient.newHttpClient()

    override fun createSquad(squadKey: SquadKey, name: Name, creator: AdminId): EitherNel<CreateSquadError, Squad> {
        val requestJson =
            "{\"query\":\"mutation { createSquad(squadKey: \\\"${squadKey.value}\\\", name: \\\"${name.value}\\\", creator: \\\"${creator.value}\\\") { squadKey name } }\"}"

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        logger.info("createSquad with squadKey $squadKey, name $name and creator $creator, response: ${response.body()}")

        val jsonString = response.body()
        val jsonElement = Json.parseToJsonElement(jsonString)
        val errors = jsonElement.jsonObject["errors"]
        return if (errors != null && errors.jsonArray.isNotEmpty()) {
            Left(
                errors
                    .jsonArray
                    .map {
                        val errorType = it.jsonObject["extensions"]?.jsonObject?.get(GRAPHQL_ERROR_TYPE)?.jsonPrimitive?.content
                        when (errorType) {
                            AdminNotExistingError::class.simpleName -> AdminNotExistingError(creator)
                            else -> {
                                MissionRepositoryError("Unknown error error type $errorType with content $it")
                            }
                        }
                    }
                    .toNonEmptyListOrNull()!!)
        } else {
            val createSquad = jsonElement.jsonObject["data"]?.jsonObject?.get("createSquad")
            val retrievedSquadKey = createSquad?.jsonObject?.get("squadKey")?.jsonPrimitive?.content
            val retrievedName = createSquad?.jsonObject?.get("name")?.jsonPrimitive?.content
            if (retrievedSquadKey != null && retrievedName != null) {
                Right(Squad(name, squadKey))
            } else {
                Left(nonEmptyListOf(MissionRepositoryError("Unexpected error: retrieved name '$retrievedName' and key '$retrievedSquadKey'")))
            }
        }
    }

    override fun assignSquad(
        squadKey: SquadKey,
        assignees: HeroIds,
        changeAuthor: AdminId
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers> {
        TODO("Not yet implemented")
    }

    override fun updateSquadName(
        squadKey: SquadKey,
        name: Name,
        changeAuthor: AdminId
    ): EitherNel<UpdateSquadNameError, Squad> {
        TODO("Not yet implemented")
    }

    override fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad> {
        TODO("Not yet implemented")
    }

    override fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers> {
        TODO("Not yet implemented")
    }

    override fun createMission(
        squadKey: SquadKey,
        title: Title,
        creator: HeroId
    ): EitherNel<CreateMissionError, PendingMission> {
        TODO("Not yet implemented")
    }

    override fun getMission(id: MissionId): EitherNel<GetMissionError, Mission<*>> {
        TODO("Not yet implemented")
    }

    override fun updateTitle(id: MissionId, title: Title, author: HeroId): EitherNel<UpdateTitleError, Mission<*>> {
        TODO("Not yet implemented")
    }

    override fun updateDescription(
        id: MissionId,
        description: Description,
        author: HeroId
    ): EitherNel<UpdateDescriptionError, Mission<*>> {
        TODO("Not yet implemented")
    }

    override fun assignMission(
        id: PendingMissionId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignMissionError, Mission<*>> {
        TODO("Not yet implemented")
    }

    override fun assignMission(
        id: InProgressMissionId,
        assignees: HeroIds,
        author: HeroId
    ): EitherNel<AssignMissionError, Mission<*>> {
        TODO("Not yet implemented")
    }

    override fun startWork(id: PendingMissionId, author: HeroId): EitherNel<StartWorkError, InProgressMission> {
        TODO("Not yet implemented")
    }

    override fun startWork(id: DoneMissionId, author: HeroId): EitherNel<StartWorkError, InProgressMission> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(id: InProgressMissionId, author: HeroId): EitherNel<PauseWorkError, PendingMission> {
        TODO("Not yet implemented")
    }

    override fun pauseWork(id: DoneMissionId, author: HeroId): EitherNel<PauseWorkError, PendingMission> {
        TODO("Not yet implemented")
    }

    override fun endWork(id: PendingMissionId, author: HeroId): EitherNel<EndWorkError, DoneMission> {
        TODO("Not yet implemented")
    }

    override fun endWork(id: InProgressMissionId, author: HeroId): EitherNel<EndWorkError, DoneMission> {
        TODO("Not yet implemented")
    }

    fun ensureAdminExistingOrThrow(id: String): AdminId {
        val adminId = AdminId(id).getOrElse { throw IllegalArgumentException("Invalid admin id: $it") }
        addUser(adminId, UserKind.ADMIN)
        return adminId
    }

    private fun addUser(id: UserId, kind: UserKind): Unit {
        // call GraphQL mutation addUser and check response
        val requestJson =
            "{\"query\":\"mutation { addUser(id: \\\"${id.value}\\\", kind: ${kind.name}) { id kind } }\"}"
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build()

        val client: HttpClient = HttpClient.newHttpClient()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        logger.info("addUser with id $id and kind $kind, response: ${response.body()}")
        val jsonString = response.body()
        if (response.statusCode() != 200) {
            throw RuntimeException("Status code ${response.statusCode()}, response '$jsonString'")
        }
    }

    fun ensureHeroExistingOrThrow(id: String): HeroId {
        val heroId = HeroId(id).getOrElse { throw IllegalArgumentException("Invalid hero id: $it") }
        addUser(heroId, UserKind.HERO)
        return heroId
    }

}
