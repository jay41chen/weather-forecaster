package com.weather.core.domain

import com.weather.core.repository.CityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSelectedCityUseCase @Inject constructor(
    private val repo: CityRepository
) {
    operator fun invoke(): Flow<String?> = repo.observeSelectedCityName()
}
