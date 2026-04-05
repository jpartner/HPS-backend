package com.hps.persistence.reference

import com.hps.domain.reference.ReferenceList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ReferenceListRepository : JpaRepository<ReferenceList, UUID> {

    fun findByKey(key: String): ReferenceList?

    @Query("""
        SELECT DISTINCT rl FROM ReferenceList rl
        LEFT JOIN FETCH rl.items i
        LEFT JOIN FETCH i.translations
        WHERE rl.isActive = true
        ORDER BY rl.key
    """)
    fun findAllActiveWithItems(): List<ReferenceList>

    @Query("""
        SELECT rl FROM ReferenceList rl
        LEFT JOIN FETCH rl.items i
        LEFT JOIN FETCH i.translations
        WHERE rl.id = :id
    """)
    fun findByIdWithItems(id: UUID): ReferenceList?
}
