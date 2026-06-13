package com.weather.core.data.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.weather.core.data.BuildConfig
import com.weather.core.data.logging.LogPortHttpLogger
import com.weather.core.data.logging.RedactingLogPort
import com.weather.core.data.network.RetrofitClient
import com.weather.core.data.network.RetryInterceptor
import com.weather.core.logging.LogPortFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideApiKeyInterceptor(): Interceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val url: HttpUrl = original.url.newBuilder()
            .addQueryParameter("appid", BuildConfig.OPEN_WEATHER_API_KEY)
            .build()
        chain.proceed(original.newBuilder().url(url).build())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKeyInterceptor: Interceptor,
        logFactory: LogPortFactory
    ): OkHttpClient {
        val redactedLog = RedactingLogPort(
            logFactory.create("OkHttp"),
            listOf(Regex("[a-fA-F0-9]{32,}") to "***REDACTED***")
        )
        val httpLogger = LogPortHttpLogger(redactedLog)
        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(RetryInterceptor())
            .addInterceptor(HttpLoggingInterceptor(httpLogger).apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofitClient(retrofit: Retrofit): RetrofitClient {
        return retrofit.create(RetrofitClient::class.java)
    }
}
