package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createSquadKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractTaskTest<Id : TaskId, T : Task<Id>> {

    @Test
    fun `updateTitle should return a new task with the updated title`() {
        val task = createTaskOrThrow("squadKey", "taskId", "title", "description")
        val newTitle = Title("new title").getOrElse { throw RuntimeException("new title should be valid") }

        val updatedTask = task.updateTitle(newTitle)

        assertEquals(newTitle, updatedTask.title)
    }

    private fun createTaskOrThrow(
        squadKey: String,
        taskId: String,
        title: String,
        description: String
    ): T {
        val squad = createSquadKeyOrThrow(squadKey)
        return createTaskOrThrow(
            createTaskIdOrThrow(squad, taskId),
            createTitleOrThrow(title),
            createDescriptionOrThrow(description)
        )
    }

    abstract fun createTaskIdOrThrow(squad: SquadKey, taskId: String): Id

    abstract fun createTaskOrThrow(
        id: Id,
        title: Title,
        description: Description,
        assignees: HeroIds = HeroIds.empty
    ): T
}
