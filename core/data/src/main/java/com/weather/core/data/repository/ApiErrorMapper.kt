package com.weather.core.data.repository

import com.weather.core.model.ApiError
import com.weather.core.model.RateLimitException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Exception.toApiError(): ApiError = when (this) {
    is RateLimitException -> ApiError.RateLimited(retryAfterSeconds)
    is UnknownHostException, is ConnectException, is SocketTimeoutException -> ApiError.NetworkUnavailable
    is HttpException -> when (code()) {
        401 -> ApiError.InvalidApiKey
        404 -> ApiError.CityNotFound
        in 500..599 -> ApiError.ServerError
        else -> ApiError.Unknown(message())
    }
    is SerializationException -> ApiError.ParseError
    is java.io.IOException -> ApiError.ServerError
    else -> ApiError.Unknown(message ?: "Unknown error")
}
