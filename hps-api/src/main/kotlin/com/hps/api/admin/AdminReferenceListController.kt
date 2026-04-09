package com.hps.api.admin

import com.hps.common.exception.BadRequestException
import com.hps.common.exception.NotFoundException
import com.hps.domain.reference.ReferenceList
import com.hps.domain.reference.ReferenceListItem
import com.hps.domain.reference.ReferenceListItemTranslation
import com.hps.persistence.reference.ReferenceListRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

// --- DTOs ---

data class ReferenceListDto(
    val id: UUID,
    val key: String,
    val name: String,
    val isActive: Boolean,
    val items: List<ReferenceListItemDto>
)

data class ReferenceListItemDto(
    val id: UUID? = null,
    val value: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val translations: List<ReferenceListItemTranslationDto> = emptyList()
)

data class ReferenceListItemTranslationDto(
    val lang: String,
    val label: String
)

data class CreateReferenceListRequest(
    val key: String,
    val name: String,
    val items: List<ReferenceListItemDto> = emptyList()
)

data class UpdateReferenceListRequest(
    val name: String? = null,
    val isActive: Boolean? = null,
    val items: List<ReferenceListItemDto>? = null
)

// --- Controller ---

@RestController
@RequestMapping("/api/v1/admin/reference-lists")
@Transactional(readOnly = true)
class AdminReferenceListController(
    private val repository: ReferenceListRepository
) {

    @GetMapping
    fun list(): List<ReferenceListDto> =
        repository.findAllActiveWithItems().map { it.toDto() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ReferenceListDto {
        val list = repository.findByIdWithItems(id)
            ?: throw NotFoundException("ReferenceList", id)
        return list.toDto()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    fun create(@RequestBody request: CreateReferenceListRequest): ReferenceListDto {
        if (repository.findByKey(request.key) != null) {
            throw BadRequestException("Reference list with key '${request.key}' already exists")
        }

        val list = ReferenceList(key = request.key, name = request.name)

        for ((idx, itemDto) in request.items.withIndex()) {
            val item = ReferenceListItem(
                referenceList = list,
                value = itemDto.value,
                sortOrder = itemDto.sortOrder.takeIf { it > 0 } ?: idx,
                isActive = itemDto.isActive
            )
            for (t in itemDto.translations) {
                item.translations.add(
                    ReferenceListItemTranslation(item = item, lang = t.lang, label = t.label)
                )
            }
            list.items.add(item)
        }

        repository.save(list)
        return list.toDto()
    }

    @PutMapping("/{id}")
    @Transactional
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateReferenceListRequest
    ): ReferenceListDto {
        val list = repository.findByIdWithItems(id)
            ?: throw NotFoundException("ReferenceList", id)

        request.name?.let { list.name = it }
        request.isActive?.let { list.isActive = it }

        if (request.items != null) {
            list.items.clear()
            repository.saveAndFlush(list)

            for ((idx, itemDto) in request.items.withIndex()) {
                val item = ReferenceListItem(
                    referenceList = list,
                    value = itemDto.value,
                    sortOrder = itemDto.sortOrder.takeIf { it > 0 } ?: idx,
                    isActive = itemDto.isActive
                )
                for (t in itemDto.translations) {
                    item.translations.add(
                        ReferenceListItemTranslation(item = item, lang = t.lang, label = t.label)
                    )
                }
                list.items.add(item)
            }
        }

        repository.save(list)
        return list.toDto()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun delete(@PathVariable id: UUID) {
        val list = repository.findById(id)
            .orElseThrow { NotFoundException("ReferenceList", id) }
        list.isActive = false
        repository.save(list)
    }

    private fun ReferenceList.toDto() = ReferenceListDto(
        id = id,
        key = key,
        name = name,
        isActive = isActive,
        items = items.sortedBy { it.sortOrder }.map { item ->
            ReferenceListItemDto(
                id = item.id,
                value = item.value,
                sortOrder = item.sortOrder,
                isActive = item.isActive,
                translations = item.translations.map { t ->
                    ReferenceListItemTranslationDto(lang = t.lang, label = t.label)
                }
            )
        }
    )
}
