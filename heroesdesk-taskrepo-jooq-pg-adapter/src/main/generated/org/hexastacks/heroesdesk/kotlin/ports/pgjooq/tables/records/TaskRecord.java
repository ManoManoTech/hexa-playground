/*
 * This file is generated by jOOQ.
 */
package org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records;


import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.enums.Taskstatus;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.Task;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TaskRecord extends UpdatableRecordImpl<TaskRecord> implements Record5<String, String, String, String, Taskstatus> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>heroesdeskschema.Task.id</code>.
     */
    public void setId(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>heroesdeskschema.Task.id</code>.
     */
    public String getId() {
        return (String) get(0);
    }

    /**
     * Setter for <code>heroesdeskschema.Task.squad_key</code>.
     */
    public void setSquadKey(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>heroesdeskschema.Task.squad_key</code>.
     */
    public String getSquadKey() {
        return (String) get(1);
    }

    /**
     * Setter for <code>heroesdeskschema.Task.title</code>.
     */
    public void setTitle(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>heroesdeskschema.Task.title</code>.
     */
    public String getTitle() {
        return (String) get(2);
    }

    /**
     * Setter for <code>heroesdeskschema.Task.description</code>.
     */
    public void setDescription(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>heroesdeskschema.Task.description</code>.
     */
    public String getDescription() {
        return (String) get(3);
    }

    /**
     * Setter for <code>heroesdeskschema.Task.status</code>.
     */
    public void setStatus(Taskstatus value) {
        set(4, value);
    }

    /**
     * Getter for <code>heroesdeskschema.Task.status</code>.
     */
    public Taskstatus getStatus() {
        return (Taskstatus) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<String, String, String, String, Taskstatus> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<String, String, String, String, Taskstatus> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return Task.TASK.ID;
    }

    @Override
    public Field<String> field2() {
        return Task.TASK.SQUAD_KEY;
    }

    @Override
    public Field<String> field3() {
        return Task.TASK.TITLE;
    }

    @Override
    public Field<String> field4() {
        return Task.TASK.DESCRIPTION;
    }

    @Override
    public Field<Taskstatus> field5() {
        return Task.TASK.STATUS;
    }

    @Override
    public String component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getSquadKey();
    }

    @Override
    public String component3() {
        return getTitle();
    }

    @Override
    public String component4() {
        return getDescription();
    }

    @Override
    public Taskstatus component5() {
        return getStatus();
    }

    @Override
    public String value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getSquadKey();
    }

    @Override
    public String value3() {
        return getTitle();
    }

    @Override
    public String value4() {
        return getDescription();
    }

    @Override
    public Taskstatus value5() {
        return getStatus();
    }

    @Override
    public TaskRecord value1(String value) {
        setId(value);
        return this;
    }

    @Override
    public TaskRecord value2(String value) {
        setSquadKey(value);
        return this;
    }

    @Override
    public TaskRecord value3(String value) {
        setTitle(value);
        return this;
    }

    @Override
    public TaskRecord value4(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public TaskRecord value5(Taskstatus value) {
        setStatus(value);
        return this;
    }

    @Override
    public TaskRecord values(String value1, String value2, String value3, String value4, Taskstatus value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TaskRecord
     */
    public TaskRecord() {
        super(Task.TASK);
    }

    /**
     * Create a detached, initialised TaskRecord
     */
    public TaskRecord(String id, String squadKey, String title, String description, Taskstatus status) {
        super(Task.TASK);

        setId(id);
        setSquadKey(squadKey);
        setTitle(title);
        setDescription(description);
        setStatus(status);
        resetChangedOnNotNull();
    }
}