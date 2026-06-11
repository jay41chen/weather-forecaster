package com.weather.core.model

sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(
        val message: String,
        val apiError: ApiError = ApiError.Unknown(message),
        val throwable: Throwable? = null
    ) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
