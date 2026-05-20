package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EnrollmentAlreadyIneligibleException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.NotFoundException
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class EnrollmentEntityRepositoryImpl(
    private val jpaRepository: EnrollmentJpaRepository
) : EnrollmentEntityRepository {

    override fun save(enrollment: Enrollment): Enrollment =
        jpaRepository.save(EnrollmentEntity.from(enrollment)).toDomain()

    override fun updateStatus(id: UUID, reason: String): Enrollment {
        val entity = jpaRepository.findById(id).orElse(null)
            ?: throw NotFoundException("Enrollment not found")
        if (entity.status == EnrollmentStatus.INELIGIBLE)
            throw EnrollmentAlreadyIneligibleException("Enrollment is already ineligible")
        entity.status = EnrollmentStatus.INELIGIBLE
        entity.reason = reason
        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: UUID): Enrollment? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByChildNric(childNric: String): List<Enrollment> =
        jpaRepository.findByChildNric(childNric).map { it.toDomain() }
}
