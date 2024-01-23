import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target

fun main() {

    fun prefixRule(context: String) = MatcherRule()
        .withTransform(
            MatcherTransformType.PASCAL
        )
        .withExpression("Raw_\$0_$context")
    DbAccess.postgreSQLContainer()
        .use { container ->
            container.start()
            val configuration = Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver(container.driverClassName)
                        .withUrl(container.getJdbcUrl())
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
                                .withInputSchema(DbAccess.dbSchema)
                        )
                        .withTarget(
                            Target()
                                .withPackageName("org.jooq.codegen.maven.example")
                                .withDirectory("todoModel-db/src/main/generated/")
                        )
                        .withGenerate(
                            Generate()
                                .withComments(true)
                        )
                        .withStrategy(
                            Strategy()
                                .withMatchers(
                                    Matchers()
                                        .withTables(
                                            MatchersTableType()
                                                .withRecordClass(prefixRule("Record")),
                                            MatchersTableType()
                                                .withTableClass(prefixRule("Table")),
                                        )
                                )
                        )
                )
            GenerationTool.generate(configuration)
            container.stop()
        }

}
