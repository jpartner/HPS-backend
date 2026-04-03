package com.hps.api.category

import java.util.UUID

data class CategoryDto(
    val id: UUID,
    val name: String,
    val icon: String?,
    val slug: String?,
    val imageUrl: String?,
    val children: List<CategoryDto> = emptyList()
)
