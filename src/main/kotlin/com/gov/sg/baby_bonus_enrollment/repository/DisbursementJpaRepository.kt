package com.gov.sg.baby_bonus_enrollment.repository

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DisbursementJpaRepository : JpaRepository<DisbursementEntity, UUID> {
    fun findByEnrollmentId(enrollmentId: UUID): DisbursementEntity?
}
