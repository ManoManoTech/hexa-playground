package org.hexastacks.heroesdesk.kotlin.impl.task

import arrow.core.EitherNel

class DeletedTaskIdTest : AbstractTaskIdTest<DeletedTaskId>() {
    override fun createStringValue(value: String): EitherNel<TaskId.TaskIdError, DeletedTaskId> = DeletedTaskId(value)

}