package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.*
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createDescriptionOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createScopeOrThow
import org.hexastacks.heroesdesk.kotlin.impl.TestUtils.createTitleOrThrow
import org.hexastacks.heroesdesk.kotlin.impl.scope.Scope
import org.hexastacks.heroesdesk.kotlin.impl.user.Hero
import org.hexastacks.heroesdesk.kotlin.impl.user.Heroes
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractTaskTest<Id : TaskId, T : Task<Id>> {

    @Test
    fun `updateTitle should return a new task with the updated title`() {
        val task = createTaskOrThrow("scopeKey", "taskId", "title", "description", "creator")
        val newTitle = Title("new title").getOrElse { throw RuntimeException("new title should be valid") }

        val updatedTask = task.updateTitle(newTitle)

        assertEquals(newTitle, updatedTask.title)
    }

    private fun createTaskOrThrow(scopeKey: String, taskId: String, title: String, description: String, creator: String): T =
        createTaskOrThrow(
            createScopeOrThow(scopeKey),
            createTaskIdOrThrow(taskId),
            createTitleOrThrow(title),
            createDescriptionOrThrow(description),
            Hero(
                TestUtils.createHeroNameOrThrow(creator),
                TestUtils.createHeroIdOrThrow(creator)
            )
        )

    abstract fun createTaskIdOrThrow(taskId: String): Id

    abstract fun createTaskOrThrow(
        scope: Scope,
        id: Id,
        title: Title,
        description: Description,
        creator: Hero,
        assignees: Heroes = Heroes.EMPTY_HEROES
    ): T
}
