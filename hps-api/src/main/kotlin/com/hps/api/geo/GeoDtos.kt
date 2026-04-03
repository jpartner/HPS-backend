package com.hps.api.geo

import java.util.UUID

data class CountryDto(
    val id: UUID,
    val isoCode: String,
    val phonePrefix: String?,
    val name: String
)

data class RegionDto(
    val id: UUID,
    val code: String?,
    val name: String,
    val latitude: Double?,
    val longitude: Double?
)

data class CityDto(
    val id: UUID,
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val population: Int?
)

data class AreaDto(
    val id: UUID,
    val name: String,
    val latitude: Double?,
    val longitude: Double?
)
