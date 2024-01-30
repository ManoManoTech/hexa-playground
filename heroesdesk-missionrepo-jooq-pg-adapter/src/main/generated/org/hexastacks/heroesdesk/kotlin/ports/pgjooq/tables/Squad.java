/*
 * This file is generated by jOOQ.
 */
package org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables;


import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Heroesdeskschema;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Keys;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.SquadRecord;
import org.jooq.Check;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Squad extends TableImpl<SquadRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>heroesdeskschema.Squad</code>
     */
    public static final Squad SQUAD = new Squad();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SquadRecord> getRecordType() {
        return SquadRecord.class;
    }

    /**
     * The column <code>heroesdeskschema.Squad.key</code>.
     */
    public final TableField<SquadRecord, String> KEY = createField(DSL.name("key"), SQLDataType.VARCHAR(36).nullable(false), this, "");

    /**
     * The column <code>heroesdeskschema.Squad.name</code>.
     */
    public final TableField<SquadRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    private Squad(Name alias, Table<SquadRecord> aliased) {
        this(alias, aliased, null);
    }

    private Squad(Name alias, Table<SquadRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>heroesdeskschema.Squad</code> table reference
     */
    public Squad(String alias) {
        this(DSL.name(alias), SQUAD);
    }

    /**
     * Create an aliased <code>heroesdeskschema.Squad</code> table reference
     */
    public Squad(Name alias) {
        this(alias, SQUAD);
    }

    /**
     * Create a <code>heroesdeskschema.Squad</code> table reference
     */
    public Squad() {
        this(DSL.name("Squad"), null);
    }

    public <O extends Record> Squad(Table<O> child, ForeignKey<O, SquadRecord> key) {
        super(child, key, SQUAD);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Heroesdeskschema.HEROESDESKSCHEMA;
    }

    @Override
    public UniqueKey<SquadRecord> getPrimaryKey() {
        return Keys.PK_SQUAD;
    }

    @Override
    public List<UniqueKey<SquadRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.CHK_NAME_UNIQUE);
    }

    @Override
    public List<Check<SquadRecord>> getChecks() {
        return Arrays.asList(
            Internal.createCheck(this, DSL.name("CHK_key_LENGTH"), "((char_length('key'::text) >= 1))", true),
            Internal.createCheck(this, DSL.name("CHK_name_MIN_LENGTH"), "((char_length('name'::text) >= 1))", true)
        );
    }

    @Override
    public Squad as(String alias) {
        return new Squad(DSL.name(alias), this);
    }

    @Override
    public Squad as(Name alias) {
        return new Squad(alias, this);
    }

    @Override
    public Squad as(Table<?> alias) {
        return new Squad(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Squad rename(String name) {
        return new Squad(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Squad rename(Name name) {
        return new Squad(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Squad rename(Table<?> name) {
        return new Squad(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}