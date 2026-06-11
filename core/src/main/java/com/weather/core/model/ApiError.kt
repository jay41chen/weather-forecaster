package com.weather.core.model

sealed interface ApiError {
    data object NetworkUnavailable : ApiError
    data object InvalidApiKey : ApiError
    data object CityNotFound : ApiError
    data class RateLimited(val retryAfterSeconds: Long?) : ApiError
    data object ServerError : ApiError
    data object ParseError : ApiError
    data class Unknown(val message: String) : ApiError
}
