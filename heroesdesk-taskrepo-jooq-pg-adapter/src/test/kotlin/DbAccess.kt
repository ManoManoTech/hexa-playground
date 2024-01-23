import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

object DbAccess {

    private const val dbName = "tododb" // also used in dbInit.sql
    const val dbSchema = "todoschema" // also used in dbInit.sql
    private  const val dbUsername = "testUser"
    private  const val dbPassword = "testPass"

    fun postgreSQLContainer(): PostgreSQLContainer<*> = PostgreSQLContainer("postgres:latest")
        .withDatabaseName(dbName)
        .withUsername(dbUsername)
        .withPassword(dbPassword)
        .withInitScript("dbInit.sql")

    fun createDslContext(container: PostgreSQLContainer<*>): DSLContext {
        val conn = DriverManager.getConnection(
            container.getJdbcUrl(),
            container.username,
            container.password
        )
        val dslContext = DSL.using(conn, SQLDialect.POSTGRES)
        dslContext.setSchema(dbSchema).execute()
        return dslContext
    }
}