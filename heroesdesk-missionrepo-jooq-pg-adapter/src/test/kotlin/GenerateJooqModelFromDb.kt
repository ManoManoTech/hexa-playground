import DbAccess.createDslContext
import DbAccess.dbSchema
import org.hexastacks.heroesdesk.kotlin.squad.Name
import org.hexastacks.heroesdesk.kotlin.squad.SquadKey
import org.hexastacks.heroesdesk.kotlin.mission.Description
import org.hexastacks.heroesdesk.kotlin.mission.MissionId
import org.hexastacks.heroesdesk.kotlin.mission.Title
import org.hexastacks.heroesdesk.kotlin.user.UserId
import org.jooq.DSLContext
import org.jooq.codegen.GenerationTool
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target


fun main() {

    DbAccess.postgreSQLContainer()
        .use { container ->
            println("Starting container...")
            container.start()
            println("Creating DSL context on ${container.jdbcUrl}")
            val dslContext = createDslContext(container)
            println("Db init")
            dbDropAndInit(dslContext)
            println("Model generation")
            val configuration = Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver(container.driverClassName)
                        .withUrl(container.jdbcUrl)
                        .withUser(container.username)
                        .withPassword(container.password)
                )
                .withGenerator(
                    Generator()
                        .withDatabase(
                            Database()
                                .withName("org.jooq.meta.postgres.PostgresDatabase")
                                .withIncludes(".*")
                                .withExcludes("")
                                .withInputSchema(dbSchema)
                        )
                        .withTarget(
                            Target()
                                .withPackageName("org.hexastacks.heroesdesk.kotlin.ports.pgjooq")
                                .withDirectory("heroesdesk-missionrepo-jooq-pg-adapter/src/main/generated/")
                        )
                        .withGenerate(
                            Generate()
                                .withComments(true)
                        )
                )
            GenerationTool.generate(configuration)
            container.stop()
        }

}

fun dbDropAndInit(dslContext: DSLContext) {
    val SQUAD_TABLE = "Squad"
    val SQUAD_KEY_COLUMN = "key"
    val SQUAD_NAME_COLUMN = "name"

    val SQUAD_TO_USER_TABLE = "${SQUAD_TABLE}_User"
    val SQUAD_TO_USER_SQUAD_KEY_COLUMN = "squad_key"
    val SQUAD_TO_USER_USER_ID_COLUMN = "user_id"

    val MISSION_TABLE = "Mission"
    val MISSION_ID_COLUMN = "id"
    val MISSION_SQUAD_COLUMN = "squad_key"
    val MISSION_TITLE_COLUMN = "title"
    val MISSION_DESCRIPTION_COLUMN = "description"
    val MISSION_STATUS_COLUMN = "status"

    val MISSION_STATUS_ENUM_TYPE = "missionstatus"
    val MISSION_STATUS_PENDING = "Pending"

    val MISSION_TO_USER_TABLE = "${MISSION_TABLE}_User"
    val MISSION_TO_USER_MISSION_ID_COLUMN = "mission_id"
    val MISSION_TO_USER_USER_ID_COLUMN = "user_id"


    dslContext.dropTableIfExists(SQUAD_TO_USER_TABLE).execute()
    dslContext.dropTableIfExists(MISSION_TO_USER_TABLE).execute()
    dslContext.dropTableIfExists(MISSION_TABLE).execute()
    dslContext.dropTypeIfExists(MISSION_STATUS_ENUM_TYPE).execute()
    dslContext.dropTableIfExists(SQUAD_TABLE).execute()
    dslContext.dropSchemaIfExists(dbSchema).execute()
    dslContext.createSchema(dbSchema).execute()

    val squadKeyColumnDefinition = SQLDataType.VARCHAR(SquadKey.MAX_LENGTH).nullable(false)
    dslContext.createTable(SQUAD_TABLE)
        .column(SQUAD_KEY_COLUMN, squadKeyColumnDefinition)
        .column(SQUAD_NAME_COLUMN, SQLDataType.VARCHAR(Name.MAX_LENGTH).nullable(false))
        .constraints(
            DSL.constraint("PK_$SQUAD_TABLE").primaryKey(SQUAD_KEY_COLUMN),
            DSL.constraint("CHK_${SQUAD_KEY_COLUMN}_LENGTH").check(
                DSL.length(SQUAD_KEY_COLUMN).ge(SquadKey.MIN_LENGTH)
            ),
            DSL.constraint("CHK_${SQUAD_NAME_COLUMN}_MIN_LENGTH").check(
                DSL.length(SQUAD_NAME_COLUMN).ge(Name.MIN_LENGTH)
            ),
            DSL.constraint("CHK_${SQUAD_NAME_COLUMN}_UNIQUE").unique(SQUAD_NAME_COLUMN)
        )
        .execute()
    val userIdColumnDefinition = SQLDataType.VARCHAR(UserId.MAX_LENGTH).nullable(false)

    dslContext.createTable(SQUAD_TO_USER_TABLE)
        .column(SQUAD_TO_USER_SQUAD_KEY_COLUMN, squadKeyColumnDefinition)
        .column(SQUAD_TO_USER_USER_ID_COLUMN, userIdColumnDefinition)
        .constraints(
            DSL.constraint("PK_$SQUAD_TO_USER_TABLE")
                .primaryKey(SQUAD_TO_USER_SQUAD_KEY_COLUMN, SQUAD_TO_USER_USER_ID_COLUMN),
            DSL.constraint("FK_$SQUAD_TABLE").foreignKey(SQUAD_TO_USER_SQUAD_KEY_COLUMN)
                .references(SQUAD_TABLE, SQUAD_KEY_COLUMN),
        )
        .execute()


    dslContext.createType(MISSION_STATUS_ENUM_TYPE)
        .asEnum(MISSION_STATUS_PENDING, "InProgress", "Done")
        .execute()

    val missionIdColumnDefinition = SQLDataType.VARCHAR(MissionId.MAX_LENGTH).nullable(false)
    dslContext.createTable(MISSION_TABLE)
        .column(MISSION_ID_COLUMN, missionIdColumnDefinition)
        .column(MISSION_SQUAD_COLUMN, SQLDataType.VARCHAR(SquadKey.MAX_LENGTH).nullable(false))
        .column(MISSION_TITLE_COLUMN, SQLDataType.VARCHAR(Title.MAX_LENGTH).nullable(false))
        .column(MISSION_DESCRIPTION_COLUMN, SQLDataType.VARCHAR(Description.MAX_LENGTH).nullable(false).defaultValue(""))
        .constraints(
            DSL.constraint("PK_$MISSION_TABLE").primaryKey(MISSION_ID_COLUMN),
            DSL.constraint("CHK_${MISSION_ID_COLUMN}_MIN_LENGTH").check(
                DSL.length(MISSION_ID_COLUMN).ge(MissionId.MIN_LENGTH)
            ),
            DSL.constraint("CHK_${MISSION_TITLE_COLUMN}_MIN_LENGTH").check(
                DSL.length(MISSION_TITLE_COLUMN).ge(Title.MIN_LENGTH)
            ),
            DSL.constraint("FK_$MISSION_SQUAD_COLUMN").foreignKey(MISSION_SQUAD_COLUMN)
                .references(SQUAD_TABLE, SQUAD_KEY_COLUMN),
            DSL.constraint("CHK_${MISSION_DESCRIPTION_COLUMN}_MIN_LENGTH").check(
                DSL.length(MISSION_TITLE_COLUMN).ge(Description.MIN_LENGTH)
            ),
        )
        .execute()

    // jooq doesn't support user defined types in DDL, cf https://github.com/jOOQ/jOOQ/issues/15300
    dslContext.execute("""ALTER TABLE $dbSchema."$MISSION_TABLE" ADD $MISSION_STATUS_COLUMN $MISSION_STATUS_ENUM_TYPE DEFAULT '$MISSION_STATUS_PENDING' NOT NULL;""")

    dslContext.createTable(MISSION_TO_USER_TABLE)
        .column(MISSION_TO_USER_MISSION_ID_COLUMN, missionIdColumnDefinition)
        .column(MISSION_TO_USER_USER_ID_COLUMN, userIdColumnDefinition)
        .constraints(
            DSL.constraint("PK_$MISSION_TO_USER_TABLE")
                .primaryKey(MISSION_TO_USER_MISSION_ID_COLUMN, MISSION_TO_USER_USER_ID_COLUMN),
            DSL.constraint("FK_$MISSION_TABLE").foreignKey(MISSION_TO_USER_MISSION_ID_COLUMN)
                .references(MISSION_TABLE, MISSION_ID_COLUMN),
        )
        .execute()
}
