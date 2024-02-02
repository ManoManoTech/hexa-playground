package org.hexastacks.heroesdesk.kotlin.app.graphql

import graphql.ErrorClassification
import graphql.ErrorType.*
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import graphql.validation.ValidationError
import org.hexastacks.heroesdesk.kotlin.errors.AdminNotExistingError
import org.hexastacks.heroesdesk.kotlin.errors.SquadKeyAlreadyExistingError
import org.hexastacks.heroesdesk.kotlin.errors.SquadNameAlreadyExistingError
import org.hexastacks.heroesdesk.kotlin.misc.ErrorMessage
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component


@Component
class ExceptionResolver : DataFetcherExceptionResolverAdapter() {

    companion object {
        const val HD_ERROR_TYPE = "HD_ERROR_TYPE"

        const val GRAPHQL_ERROR_FIELD_ADMIN_ID = "adminId"
        const val GRAPHQL_ERROR_FIELD_SCOPE_NAME = "squad_name"
    }

    override fun resolveToMultipleErrors(ex: Throwable, env: DataFetchingEnvironment): MutableList<GraphQLError>? =
        if (ex is HeroesDeskControllerException) {
            ex.errorMessages
                .map {
                    when (it) {
                        is AdminNotExistingError -> {
                            val extensions = initExtensions(it) + (GRAPHQL_ERROR_FIELD_ADMIN_ID to it.adminId.value)
                            createGraphQlError(ErrorType.NOT_FOUND, it.message, env, extensions)
                        }

                        is SquadNameAlreadyExistingError -> {
                            val extensions = initExtensions(it) + (GRAPHQL_ERROR_FIELD_SCOPE_NAME to it.name.value)
                            createGraphQlError(ErrorType.FORBIDDEN, it.message, env, extensions)
                        }

                        is SquadKeyAlreadyExistingError -> {
                            val extensions = initExtensions(it) + (GRAPHQL_ERROR_FIELD_SCOPE_NAME to it.id.value)
                            createGraphQlError(ValidationError, it.message, env, extensions)
                        }

                        else -> {
                            GraphqlErrorBuilder.newError()
                                .errorType(ErrorType.INTERNAL_ERROR)
                                .message(it.message)
                                .path(env.executionStepInfo.path)
                                .location(env.getField().sourceLocation)
                                .build()
                        }
                    }
                }
                .toMutableList()
        } else null

    private fun initExtensions(it: ErrorMessage) = mapOf(
        HD_ERROR_TYPE to it::class.simpleName
    )

    private fun createGraphQlError(
        errorType: ErrorClassification,
        message: String,
        env: DataFetchingEnvironment,
        extensions: Map<String, String?>
    ): GraphQLError =
        GraphqlErrorBuilder.newError()
            .errorType(errorType)
            .message(message)
            .path(env.executionStepInfo.path)
            .location(env.field.sourceLocation)
            .extensions(
                extensions
            )
            .build()

}