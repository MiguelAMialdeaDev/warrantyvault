package com.miguelcode.warrantyvault.domain.model

import kotlinx.coroutines.CancellationException

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    companion object {
        inline fun <T> runCatching(block: () -> T): Result<T> =
            try {
                Success(block())
            } catch (e: CancellationException) {
                // La cancelación no es un fallo del bloque: es la corrutina llamante
                // terminando. Convertirla en Error hacía que quien llama siguiera
                // ejecutando código después del punto de cancelación, y el repositorio
                // envuelve aquí TODAS las escrituras del DAO.
                throw e
            } catch (e: Exception) {
                Error(e)
            }
    }
}
