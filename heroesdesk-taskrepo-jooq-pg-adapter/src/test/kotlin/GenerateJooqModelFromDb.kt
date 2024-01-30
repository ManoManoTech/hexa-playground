import DbAccess.createDslContext
import DbAccess.dbSchema
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.task.Description
import org.hexastacks.heroesdesk.kotlin.impl.task.TaskId
import org.hexastacks.heroesdesk.kotlin.impl.task.Title
import org.hexastacks.heroesdesk.kotlin.impl.user.UserId
import org.jooq.DSLContext
import org.jooq.codegen.GenerationTool
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target


fun main() {

    fun prefixRule(context: String) = MatcherRule()
        .withExpression("\$0$context")

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
                                .withDirectory("heroesdesk-taskrepo-jooq-pg-adapter/src/main/generated/")
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
    val SCOPE_TABLE = "Scope"
    val SCOPE_KEY_COLUMN = "key"
    val SCOPE_NAME_COLUMN = "name"

    val SCOPE_TO_USER_TABLE = "${SCOPE_TABLE}_User"
    val SCOPE_TO_USER_SCOPE_KEY_COLUMN = "scope_key"
    val SCOPE_TO_USER_USER_ID_COLUMN = "user_id"

    val TASK_TABLE = "Task"
    val TASK_ID_COLUMN = "id"
    val TASK_SCOPE_COLUMN = "scope_key"
    val TASK_TITLE_COLUMN = "title"
    val TASK_DESCRIPTION_COLUMN = "description"
    val TASK_STATUS_COLUMN = "status"

    val TASK_STATUS_ENUM_TYPE = "taskstatus"
    val TASK_STATUS_PENDING = "Pending"

    val TASK_TO_USER_TABLE = "${TASK_TABLE}_User"
    val TASK_TO_USER_TASK_ID_COLUMN = "task_id"
    val TASK_TO_USER_USER_ID_COLUMN = "user_id"


    dslContext.dropTableIfExists(SCOPE_TO_USER_TABLE).execute()
    dslContext.dropTableIfExists(TASK_TO_USER_TABLE).execute()
    dslContext.dropTableIfExists(TASK_TABLE).execute()
    dslContext.dropTypeIfExists(TASK_STATUS_ENUM_TYPE).execute()
    dslContext.dropTableIfExists(SCOPE_TABLE).execute()
    dslContext.dropSchemaIfExists(dbSchema).execute()
    dslContext.createSchema(dbSchema).execute()

    val scopeKeyColumnDefinition = SQLDataType.VARCHAR(ScopeKey.MAX_LENGTH).nullable(false)
    dslContext.createTable(SCOPE_TABLE)
        .column(SCOPE_KEY_COLUMN, scopeKeyColumnDefinition)
        .column(SCOPE_NAME_COLUMN, SQLDataType.VARCHAR(Name.MAX_LENGTH).nullable(false))
        .constraints(
            DSL.constraint("PK_SCOPE").primaryKey(SCOPE_KEY_COLUMN),
            DSL.constraint("CHK_${SCOPE_KEY_COLUMN}_LENGTH").check(
                DSL.length(SCOPE_KEY_COLUMN).ge(ScopeKey.MIN_LENGTH)
            ),
            DSL.constraint("CHK_${SCOPE_NAME_COLUMN}_MIN_LENGTH").check(
                DSL.length(SCOPE_NAME_COLUMN).ge(Name.MIN_LENGTH)
            ),
            DSL.constraint("CHK_${SCOPE_NAME_COLUMN}_UNIQUE").unique(SCOPE_NAME_COLUMN)
        )
        .execute()
    val userIdColumnDefinition = SQLDataType.VARCHAR(UserId.MAX_LENGTH).nullable(false)

    dslContext.createTable(SCOPE_TO_USER_TABLE)
        .column(SCOPE_TO_USER_SCOPE_KEY_COLUMN, scopeKeyColumnDefinition)
        .column(SCOPE_TO_USER_USER_ID_COLUMN, userIdColumnDefinition)
        .constraints(
            DSL.constraint("PK_$SCOPE_TO_USER_TABLE")
                .primaryKey(SCOPE_TO_USER_SCOPE_KEY_COLUMN, SCOPE_TO_USER_USER_ID_COLUMN),
            DSL.constraint("FK_$SCOPE_TABLE").foreignKey(SCOPE_TO_USER_SCOPE_KEY_COLUMN)
                .references(SCOPE_TABLE, SCOPE_KEY_COLUMN),
        )
        .execute()


    dslContext.createType(TASK_STATUS_ENUM_TYPE)
        .asEnum(TASK_STATUS_PENDING, "InProgress", "Done")
        .execute()

    val taskIdColumnDefinition = SQLDataType.VARCHAR(TaskId.MAX_LENGTH).nullable(false)
    dslContext.createTable(TASK_TABLE)
        .column(TASK_ID_COLUMN, taskIdColumnDefinition)
        .column(TASK_SCOPE_COLUMN, SQLDataType.VARCHAR(ScopeKey.MAX_LENGTH).nullable(false))
        .column(TASK_TITLE_COLUMN, SQLDataType.VARCHAR(Title.MAX_LENGTH).nullable(false))
        .column(TASK_DESCRIPTION_COLUMN, SQLDataType.VARCHAR(Description.MAX_LENGTH).nullable(false).defaultValue(""))
        .constraints(
            DSL.constraint("PK_$TASK_TABLE").primaryKey(TASK_ID_COLUMN),
            DSL.constraint("CHK_${TASK_ID_COLUMN}_MIN_LENGTH").check(
                DSL.length(TASK_ID_COLUMN).ge(TaskId.MIN_LENGTH)
            ),
            DSL.constraint("CHK_${TASK_TITLE_COLUMN}_MIN_LENGTH").check(
                DSL.length(TASK_TITLE_COLUMN).ge(Title.MIN_LENGTH)
            ),
            DSL.constraint("FK_$TASK_SCOPE_COLUMN").foreignKey(TASK_SCOPE_COLUMN)
                .references(SCOPE_TABLE, SCOPE_KEY_COLUMN),
            DSL.constraint("CHK_${TASK_DESCRIPTION_COLUMN}_MIN_LENGTH").check(
                DSL.length(TASK_TITLE_COLUMN).ge(Description.MIN_LENGTH)
            ),
        )
        .execute()

    // jooq doesn't support user defined types in DDL, cf https://github.com/jOOQ/jOOQ/issues/15300
    dslContext.execute("""ALTER TABLE $dbSchema."$TASK_TABLE" ADD $TASK_STATUS_COLUMN $TASK_STATUS_ENUM_TYPE DEFAULT '$TASK_STATUS_PENDING' NOT NULL;""")

    dslContext.createTable(TASK_TO_USER_TABLE)
        .column(TASK_TO_USER_TASK_ID_COLUMN, taskIdColumnDefinition)
        .column(TASK_TO_USER_USER_ID_COLUMN, userIdColumnDefinition)
        .constraints(
            DSL.constraint("PK_$TASK_TO_USER_TABLE")
                .primaryKey(TASK_TO_USER_TASK_ID_COLUMN, TASK_TO_USER_USER_ID_COLUMN),
            DSL.constraint("FK_$TASK_TABLE").foreignKey(TASK_TO_USER_TASK_ID_COLUMN)
                .references(TASK_TABLE, TASK_ID_COLUMN),
        )
        .execute()
}
