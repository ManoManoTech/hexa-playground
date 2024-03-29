/*
 * This file is generated by jOOQ.
 */
package org.hexastacks.heroesdesk.kotlin.ports.pgjooq;


import java.util.Arrays;
import java.util.List;

import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.Mission;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.MissionUser;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.Squad;
import org.hexastacks.heroesdesk.kotlin.ports.pgjooq.tables.SquadUser;
import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Heroesdeskschema extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>heroesdeskschema</code>
     */
    public static final Heroesdeskschema HEROESDESKSCHEMA = new Heroesdeskschema();

    /**
     * The table <code>heroesdeskschema.Mission</code>.
     */
    public final Mission MISSION = Mission.MISSION;

    /**
     * The table <code>heroesdeskschema.Mission_User</code>.
     */
    public final MissionUser MISSION_USER = MissionUser.MISSION_USER;

    /**
     * The table <code>heroesdeskschema.Squad</code>.
     */
    public final Squad SQUAD = Squad.SQUAD;

    /**
     * The table <code>heroesdeskschema.Squad_User</code>.
     */
    public final SquadUser SQUAD_USER = SquadUser.SQUAD_USER;

    /**
     * No further instances allowed
     */
    private Heroesdeskschema() {
        super("heroesdeskschema", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Mission.MISSION,
            MissionUser.MISSION_USER,
            Squad.SQUAD,
            SquadUser.SQUAD_USER
        );
    }
}
