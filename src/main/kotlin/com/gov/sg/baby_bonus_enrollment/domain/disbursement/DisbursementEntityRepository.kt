package com.gov.sg.baby_bonus_enrollment.domain.disbursement

import java.util.UUID

interface DisbursementEntityRepository {
    fun save(disbursement: Disbursement): Disbursement
    fun findById(id: UUID): Disbursement?
    fun findByEnrollmentId(enrollmentId: UUID): Disbursement?
}
