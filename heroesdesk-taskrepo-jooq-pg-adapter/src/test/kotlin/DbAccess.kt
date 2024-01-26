import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager


object DbAccess {

    private const val exposedPort = 5432
    private const val dbName = "heroesdb"
    const val dbSchema = "heroesdeskschema"
    private  const val dbUsername = "testUser"
    private  const val dbPassword = "testPass"

    fun postgreSQLContainer(): PostgreSQLContainer<*> = PostgreSQLContainer("postgres:latest")
        .withExposedPorts(exposedPort)
        .withDatabaseName(dbName)
        .withUsername(dbUsername)
        .withPassword(dbPassword)

    fun createDslContext(container: PostgreSQLContainer<*>): DSLContext {
        Thread.sleep(5000)
        val conn = DriverManager.getConnection(
            container.jdbcUrl,
            container.username,
            container.password
        )
        val dslContext = DSL.using(conn, SQLDialect.POSTGRES)
        dslContext.setSchema(dbSchema).execute()
        return dslContext
    }
}