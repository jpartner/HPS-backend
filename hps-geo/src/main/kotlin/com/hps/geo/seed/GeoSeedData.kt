package com.hps.geo.seed

data class CountrySeed(
    val isoCode: String,
    val phonePrefix: String,
    val translations: Map<String, String>,
    val regions: List<RegionSeed>
)

data class RegionSeed(
    val code: String,
    val latitude: Double,
    val longitude: Double,
    val translations: Map<String, String>,
    val cities: List<CitySeed>
)

data class CitySeed(
    val latitude: Double,
    val longitude: Double,
    val population: Int,
    val translations: Map<String, String>
)

data class CategorySeed(
    val icon: String?,
    val sortOrder: Int,
    val translations: Map<String, String>,
    val children: List<CategorySeed> = emptyList()
)
