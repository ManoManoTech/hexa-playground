package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.getOrElse
import org.hexastacks.heroesdesk.kotlin.impl.*
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractTaskTest<Id : TaskId, T : Task<Id>> {

    @Test
    fun `updateTitle should return a new task with the updated title`() {
        val task = createTaskOrThrow("taskId", "title", "description", "creator")
        val newTitle = Title("new title").getOrElse { throw RuntimeException("new title should be valid") }

        val updatedTask = task.updateTitle(newTitle)

        assertEquals(newTitle, updatedTask.title)
    }

    private fun createTaskOrThrow(taskId: String, title: String, description: String, creator: String): T =
        createTaskOrThrow(
            createTaskIdOrThrow(taskId),
            Title(title).getOrElse { throw RuntimeException("title should be valid") },
            Description(description).getOrElse { throw RuntimeException("description should be valid") },
            Hero(
                HeroName(creator).getOrElse { throw RuntimeException("$creator should be valid") },
                HeroId(creator).getOrElse { throw RuntimeException("$creator should be valid") })
        )

    abstract fun createTaskIdOrThrow(taskId: String): Id

    abstract fun createTaskOrThrow(id: Id, title: Title, description: Description, creator: Hero): T
}
