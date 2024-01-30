package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createScopeKeyOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.user.HeroIds
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractTaskTest<Id : TaskId, T : Task<Id>> {

    @Test
    fun `updateTitle should return a new task with the updated title`() {
        val task = createTaskOrThrow("scopeKey", "taskId", "title", "description")
        val newTitle = Title("new title").getOrElse { throw RuntimeException("new title should be valid") }

        val updatedTask = task.updateTitle(newTitle)

        assertEquals(newTitle, updatedTask.title)
    }

    private fun createTaskOrThrow(
        scopeKey: String,
        taskId: String,
        title: String,
        description: String
    ): T {
        val scope = createScopeKeyOrThrow(scopeKey)
        return createTaskOrThrow(
            createTaskIdOrThrow(scope, taskId),
            createTitleOrThrow(title),
            createDescriptionOrThrow(description)
        )
    }

    abstract fun createTaskIdOrThrow(scope: ScopeKey, taskId: String): Id

    abstract fun createTaskOrThrow(
        id: Id,
        title: Title,
        description: Description,
        assignees: HeroIds = HeroIds.empty
    ): T
}
