package org.hexastacks.heroesdesk.kotlin.ports

import arrow.core.EitherNel
import arrow.core.raise.either
import org.hexastacks.heroesdesk.kotlin.errors.*
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.Squad
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.squad.SquadMembers
import org.hexastacks.heroesdesk.kotlin.impl.task.*
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes

interface TaskRepository {
    fun createSquad(squadKey: SquadKey, name: Name): EitherNel<CreateSquadError, Squad>

    fun getSquad(squadKey: SquadKey): EitherNel<GetSquadError, Squad>
    fun getSquadMembers(squadKey: SquadKey): EitherNel<GetSquadMembersError, SquadMembers>

    fun updateSquadName(
        squadKey: SquadKey,
        name: Name
    ): EitherNel<UpdateSquadNameError, Squad>

    fun assignSquad(
        squadKey: SquadKey,
        assignees: Heroes
    ): EitherNel<AssignHeroesOnSquadError, SquadMembers>

    fun areHeroesInSquad(heroIds: HeroIds, squadKey: SquadKey): EitherNel<AreHeroesInSquadError, SquadMembers>
    fun areHeroesInSquad(heroIds: HeroIds, taskId: TaskId): EitherNel<AreHeroesInSquadError, Task<*>> =
        either {
            val task = getTask(taskId).bind()
            areHeroesInSquad(heroIds, task.squadKey()).bind()
            task
        }

    fun createTask(squadKey: SquadKey, title: Title): EitherNel<CreateTaskError, PendingTask>

    fun getTask(taskId: TaskId): EitherNel<GetTaskError, Task<*>>

    fun updateTitle(
        taskId: TaskId,
        title: Title
    ): EitherNel<UpdateTitleError, Task<*>>

    fun updateDescription(
        taskId: TaskId,
        description: Description
    ): EitherNel<UpdateDescriptionError, Task<*>>

    fun assignTask(
        taskId: TaskId,
        assignees: HeroIds
    ): EitherNel<AssignTaskError, Task<*>>

    fun startWork(pendingTaskId: PendingTaskId): EitherNel<StartWorkError, InProgressTask>

    fun pauseWork(inProgressTaskId: InProgressTaskId): EitherNel<PauseWorkError, PendingTask>

    fun endWork(inProgressTaskId: InProgressTaskId): EitherNel<EndWorkError, DoneTask>


}
