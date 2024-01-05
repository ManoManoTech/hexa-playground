package org.hexastacks.heroesdesk.kotlin.ports

import org.hexastacks.heroesdesk.kotlin.impl.Heroes
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId

interface InstrumentedUserRepository : UserRepository {

    /**
     * @param taskId: the task the heroes can be assigned to, no check is done on the existence of the task
     * @param heroesToCreateIfNeeded: the heroes to assign, creating them on the way if needed
     *
     */
    fun defineAssignableHeroes(taskId: TaskId, heroesToCreateIfNeeded: Heroes)
}
