/*
 * This file is generated by jOOQ.
 */
package org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables;


import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Heroesdeskschema;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Keys;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.records.SquadUserRecord;
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
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SquadUser extends TableImpl<SquadUserRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>heroesdeskschema.Squad_User</code>
     */
    public static final SquadUser SQUAD_USER = new SquadUser();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SquadUserRecord> getRecordType() {
        return SquadUserRecord.class;
    }

    /**
     * The column <code>heroesdeskschema.Squad_User.squad_key</code>.
     */
    public final TableField<SquadUserRecord, String> SQUAD_KEY = createField(DSL.name("squad_key"), SQLDataType.VARCHAR(36).nullable(false), this, "");

    /**
     * The column <code>heroesdeskschema.Squad_User.user_id</code>.
     */
    public final TableField<SquadUserRecord, String> USER_ID = createField(DSL.name("user_id"), SQLDataType.VARCHAR(36).nullable(false), this, "");

    private SquadUser(Name alias, Table<SquadUserRecord> aliased) {
        this(alias, aliased, null);
    }

    private SquadUser(Name alias, Table<SquadUserRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>heroesdeskschema.Squad_User</code> table
     * reference
     */
    public SquadUser(String alias) {
        this(DSL.name(alias), SQUAD_USER);
    }

    /**
     * Create an aliased <code>heroesdeskschema.Squad_User</code> table
     * reference
     */
    public SquadUser(Name alias) {
        this(alias, SQUAD_USER);
    }

    /**
     * Create a <code>heroesdeskschema.Squad_User</code> table reference
     */
    public SquadUser() {
        this(DSL.name("Squad_User"), null);
    }

    public <O extends Record> SquadUser(Table<O> child, ForeignKey<O, SquadUserRecord> key) {
        super(child, key, SQUAD_USER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Heroesdeskschema.HEROESDESKSCHEMA;
    }

    @Override
    public UniqueKey<SquadUserRecord> getPrimaryKey() {
        return Keys.PK_SQUAD_USER;
    }

    @Override
    public List<ForeignKey<SquadUserRecord, ?>> getReferences() {
        return Arrays.asList(Keys.SQUAD_USER__FK_SQUAD);
    }

    private transient Squad _squad;

    /**
     * Get the implicit join path to the <code>heroesdeskschema.Squad</code>
     * table.
     */
    public Squad squad() {
        if (_squad == null)
            _squad = new Squad(this, Keys.SQUAD_USER__FK_SQUAD);

        return _squad;
    }

    @Override
    public SquadUser as(String alias) {
        return new SquadUser(DSL.name(alias), this);
    }

    @Override
    public SquadUser as(Name alias) {
        return new SquadUser(alias, this);
    }

    @Override
    public SquadUser as(Table<?> alias) {
        return new SquadUser(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public SquadUser rename(String name) {
        return new SquadUser(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public SquadUser rename(Name name) {
        return new SquadUser(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public SquadUser rename(Table<?> name) {
        return new SquadUser(name.getQualifiedName(), null);
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
