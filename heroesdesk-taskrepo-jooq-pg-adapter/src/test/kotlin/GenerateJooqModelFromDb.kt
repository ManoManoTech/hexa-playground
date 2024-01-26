import DbAccess.createDslContext
import DbAccess.dbSchema
import org.hexastacks.heroesdesk.kotlin.impl.scope.Name
import org.hexastacks.heroesdesk.kotlin.impl.scope.ScopeKey
import org.hexastacks.heroesdesk.kotlin.impl.user.UserId
import org.hexastacks.heroesdesk.kotlin.impl.user.UserName
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
    val USER_TABLE = "User"
    val USER_ID_COLUMN = "id"
    val USER_NAME_COLUMN = "name"

    val SCOPE_TO_USER_TABLE = "Scope_User"
    val SCOPE_TO_USER_SCOPE_KEY_COLUMN = "scope_key"
    val SCOPE_TO_USER_USER_ID_COLUMN = "id"

    dslContext.dropTableIfExists(SCOPE_TO_USER_TABLE).execute()
    dslContext.dropTableIfExists(USER_TABLE).execute()
    dslContext.dropTableIfExists(SCOPE_TABLE).execute()
    dslContext.dropSchemaIfExists(dbSchema).execute()
    dslContext.createSchema(dbSchema).execute()

    val scopeKeyColumnDefinition = SQLDataType.VARCHAR(ScopeKey.MAX_LENGTH).nullable(false)
    dslContext.createTable(SCOPE_TABLE)
        .column(SCOPE_KEY_COLUMN, scopeKeyColumnDefinition)
        .column(SCOPE_NAME_COLUMN, SQLDataType.VARCHAR(Name.MAX_LENGTH).nullable(false))
        .constraints(
            DSL.constraint("PK_SCOPE").primaryKey(SCOPE_KEY_COLUMN),
            DSL.constraint("CHK_SCOPE_KEY_MIN_LENGTH").check(
                DSL.length(SCOPE_KEY_COLUMN).ge(ScopeKey.MIN_LENGTH)
            ),
            DSL.constraint("CHK_SCOPE_NAME_MIN_LENGTH").check(
                DSL.length(SCOPE_NAME_COLUMN).ge(Name.MIN_LENGTH)
            ),
        )
        .execute()
    val userIdColumnDefinition  = SQLDataType.VARCHAR(UserId.MAX_LENGTH).nullable(false)
    dslContext.createTable(USER_TABLE)
        .column(USER_ID_COLUMN, userIdColumnDefinition)
        .column(USER_NAME_COLUMN, SQLDataType.VARCHAR(UserName.MAX_LENGTH).nullable(false))
        .constraints(
            DSL.constraint("PK_USER_ID").primaryKey(USER_ID_COLUMN),
            DSL.constraint("CHK_USER_ID_MIN_LENGTH").check(
                DSL.length(SCOPE_KEY_COLUMN).ge(UserId.MIN_LENGTH)
            ),
            DSL.constraint("CHK_USER_NAME_MIN_LENGTH").check(
                DSL.length(SCOPE_NAME_COLUMN).ge(UserName.MIN_LENGTH)
            ),
        )
        .execute()

    dslContext.createTable(SCOPE_TO_USER_TABLE)
        .column(SCOPE_TO_USER_SCOPE_KEY_COLUMN, scopeKeyColumnDefinition)
        .column(SCOPE_TO_USER_USER_ID_COLUMN, userIdColumnDefinition)
        .constraints(
            DSL.constraint("PK_$SCOPE_TO_USER_TABLE").primaryKey(SCOPE_TO_USER_SCOPE_KEY_COLUMN, SCOPE_TO_USER_USER_ID_COLUMN),
            DSL.constraint("FK_$SCOPE_TABLE").foreignKey(SCOPE_TO_USER_SCOPE_KEY_COLUMN).references(SCOPE_TABLE, SCOPE_KEY_COLUMN),
            DSL.constraint("FK_$USER_TABLE").foreignKey(SCOPE_TO_USER_USER_ID_COLUMN).references(USER_TABLE, USER_ID_COLUMN),
        )
        .execute()
}
