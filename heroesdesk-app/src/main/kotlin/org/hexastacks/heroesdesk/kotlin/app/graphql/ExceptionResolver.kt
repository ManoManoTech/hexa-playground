package org.hexastacks.heroesdesk.kotlin.app.graphql

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.hexastacks.heroesdesk.kotlin.errors.AdminNotExistingError
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component


@Component
class ExceptionResolver : DataFetcherExceptionResolverAdapter() {

    companion object {
        const val GRAPHQL_ERROR_TYPE = "TYPE"

        const val GRAPHQL_ERROR_FIELD_ADMIN_ID = "adminId"
    }

    override fun resolveToMultipleErrors(ex: Throwable, env: DataFetchingEnvironment): MutableList<GraphQLError>? =
        if (ex is HeroesDeskControllerException) {
            ex.errorMessages
                .map {
                    when (it) {
                        is AdminNotExistingError -> {
                            GraphqlErrorBuilder.newError()
                                .errorType(ErrorType.NOT_FOUND)
                                .message(it.message)
                                .path(env.executionStepInfo.path)
                                .location(env.field.sourceLocation)
                                .extensions(mapOf(GRAPHQL_ERROR_TYPE to AdminNotExistingError::class.simpleName, GRAPHQL_ERROR_FIELD_ADMIN_ID to it.adminId.value))
                                .build()
                        }

                        else -> {
                            GraphqlErrorBuilder.newError()
                                .errorType(ErrorType.INTERNAL_ERROR)
                                .message(it.message)
                                .path(env.executionStepInfo.path)
                                .location(env.getField().sourceLocation)
                                .build()}
                    }
                }
                .toMutableList()
        } else null

}