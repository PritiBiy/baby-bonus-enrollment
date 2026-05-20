package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EnrollmentAlreadyIneligibleException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.NotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class EnrollmentEntityRepositoryImpl(
    private val jpaRepository: EnrollmentJpaRepository
) : EnrollmentEntityRepository {

    override fun save(enrollment: Enrollment): Enrollment =
        jpaRepository.save(EnrollmentEntity.from(enrollment)).toDomain()

    @Transactional
    override fun updateStatus(id: UUID, reason: String): Enrollment {
        val affected = jpaRepository.updateStatusAndReason(id, reason)
        if (affected == 1) return jpaRepository.findById(id).orElseThrow().toDomain()
        jpaRepository.findById(id).orElse(null)
            ?: throw NotFoundException("Enrollment not found")
        throw EnrollmentAlreadyIneligibleException("Enrollment is already ineligible")
    }

    override fun findById(id: UUID): Enrollment? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByChildNric(childNric: String): List<Enrollment> =
        jpaRepository.findByChildNric(childNric).map { it.toDomain() }
}
