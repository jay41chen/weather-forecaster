package com.weather.core.model

import java.io.IOException

class RateLimitException(val retryAfterSeconds: Long?) : IOException(
    if (retryAfterSeconds != null) "Rate limit exceeded. Retry after ${retryAfterSeconds}s."
    else "Rate limit exceeded."
)
