/*
 * This file is generated by jOOQ.
 */
package org.hexastacks.heroesdesk.kotlin.ports.pgjooq;


import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.Squad;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.SquadUser;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.Task;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.TaskUser;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.SquadRecord;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.SquadUserRecord;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.TaskRecord;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.TaskUserRecord;
import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * heroesdeskschema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<SquadRecord> CHK_NAME_UNIQUE = Internal.createUniqueKey(Squad.SQUAD, DSL.name("CHK_name_UNIQUE"), new TableField[] { Squad.SQUAD.NAME }, true);
    public static final UniqueKey<SquadRecord> PK_SQUAD = Internal.createUniqueKey(Squad.SQUAD, DSL.name("PK_SQUAD"), new TableField[] { Squad.SQUAD.KEY }, true);
    public static final UniqueKey<SquadUserRecord> PK_SQUAD_USER = Internal.createUniqueKey(SquadUser.SQUAD_USER, DSL.name("PK_Squad_User"), new TableField[] { SquadUser.SQUAD_USER.SQUAD_KEY, SquadUser.SQUAD_USER.USER_ID }, true);
    public static final UniqueKey<TaskRecord> PK_TASK = Internal.createUniqueKey(Task.TASK, DSL.name("PK_Task"), new TableField[] { Task.TASK.ID }, true);
    public static final UniqueKey<TaskUserRecord> PK_TASK_USER = Internal.createUniqueKey(TaskUser.TASK_USER, DSL.name("PK_Task_User"), new TableField[] { TaskUser.TASK_USER.TASK_ID, TaskUser.TASK_USER.USER_ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<SquadUserRecord, SquadRecord> SQUAD_USER__FK_SQUAD = Internal.createForeignKey(SquadUser.SQUAD_USER, DSL.name("FK_Squad"), new TableField[] { SquadUser.SQUAD_USER.SQUAD_KEY }, Keys.PK_SQUAD, new TableField[] { Squad.SQUAD.KEY }, true);
    public static final ForeignKey<TaskRecord, SquadRecord> TASK__FK_SQUAD_KEY = Internal.createForeignKey(Task.TASK, DSL.name("FK_squad_key"), new TableField[] { Task.TASK.SQUAD_KEY }, Keys.PK_SQUAD, new TableField[] { Squad.SQUAD.KEY }, true);
    public static final ForeignKey<TaskUserRecord, TaskRecord> TASK_USER__FK_TASK = Internal.createForeignKey(TaskUser.TASK_USER, DSL.name("FK_Task"), new TableField[] { TaskUser.TASK_USER.TASK_ID }, Keys.PK_TASK, new TableField[] { Task.TASK.ID }, true);
}