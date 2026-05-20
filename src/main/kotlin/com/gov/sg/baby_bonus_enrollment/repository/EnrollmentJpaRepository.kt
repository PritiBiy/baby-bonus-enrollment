package com.gov.sg.baby_bonus_enrollment.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface EnrollmentJpaRepository : JpaRepository<EnrollmentEntity, UUID> {
    fun findByChildNric(childNric: String): List<EnrollmentEntity>

    /*
    As we had to make 2 db calls in use case layer, it can be avoided once we move to postgress with following, Returning * also returns the updated row and its not available in H2

        UPDATE enrollment
            SET status = 'INELIGIBLE',
            reason = :reason
        WHERE id = :id
        AND status != 'INELIGIBLE'
        RETURNING *
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE EnrollmentEntity e
        SET e.status = 'INELIGIBLE', e.reason = :reason
        WHERE e.id = :id
        AND e.status != 'INELIGIBLE'
    """)
    fun updateStatusAndReason(@Param("id") id: UUID, @Param("reason") reason: String): Int
}
