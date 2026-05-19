package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class EnrollmentRepositoryImpl(
    private val jpaRepository: EnrollmentJpaRepository
) : EnrollmentRepository {

    override fun save(enrollment: Enrollment): Enrollment =
        jpaRepository.save(EnrollmentEntity.from(enrollment)).toDomain()

    override fun findById(id: UUID): Enrollment? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByChildNric(childNric: String): List<Enrollment> =
        jpaRepository.findByChildNric(childNric).map { it.toDomain() }
}
