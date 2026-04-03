package com.hps.common.dto

data class PageResponse<T>(
    val data: List<T>,
    val meta: PageMeta
)

data class PageMeta(
    val page: Int,
    val pageSize: Int,
    val totalItems: Long
)
