package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DisbursementRepositoryImpl(
    private val jpaRepository: DisbursementJpaRepository
) : DisbursementRepository {

    override fun save(disbursement: Disbursement): Disbursement =
        jpaRepository.save(DisbursementEntity.from(disbursement)).toDomain()

    override fun findById(id: UUID): Disbursement? =
        jpaRepository.findById(id).orElse(null)?.toDomain()
}
