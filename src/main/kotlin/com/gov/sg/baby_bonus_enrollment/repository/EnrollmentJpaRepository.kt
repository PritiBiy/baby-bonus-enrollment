package com.gov.sg.baby_bonus_enrollment.repository

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EnrollmentJpaRepository : JpaRepository<EnrollmentEntity, UUID> {
    fun findByChildNric(childNric: String): List<EnrollmentEntity>
}
