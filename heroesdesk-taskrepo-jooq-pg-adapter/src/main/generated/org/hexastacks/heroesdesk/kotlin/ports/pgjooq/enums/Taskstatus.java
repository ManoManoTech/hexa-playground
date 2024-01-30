/*
 * This file is generated by jOOQ.
 */
package org.hexastacks.heroesdesk.kotlin.ports.pgjooq.enums;


import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.Heroesdeskschema;
import org.jooq.Catalog;
import org.jooq.EnumType;
import org.jooq.Schema;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public enum Taskstatus implements EnumType {

    Pending("Pending"),

    InProgress("InProgress"),

    Done("Done");

    private final String literal;

    private Taskstatus(String literal) {
        this.literal = literal;
    }

    @Override
    public Catalog getCatalog() {
        return getSchema().getCatalog();
    }

    @Override
    public Schema getSchema() {
        return Heroesdeskschema.HEROESDESKSCHEMA;
    }

    @Override
    public String getName() {
        return "taskstatus";
    }

    @Override
    public String getLiteral() {
        return literal;
    }

    /**
     * Lookup a value of this EnumType by its literal
     */
    public static Taskstatus lookupLiteral(String literal) {
        return EnumType.lookupLiteral(Taskstatus.class, literal);
    }
}