package org.hexastacks.heroesdesk.kotlin.impl.task

import org.hexastacks.heroesdesk.kotlin.impl.AbstractStringValueTest

abstract class AbstractTaskIdTest<Id : TaskId> : AbstractStringValueTest<Id, TaskId.TaskIdError>() {

    override val minLength: Int = TaskId.MIN_LENGTH
    override val maxLength: Int = TaskId.MAX_LENGTH
}