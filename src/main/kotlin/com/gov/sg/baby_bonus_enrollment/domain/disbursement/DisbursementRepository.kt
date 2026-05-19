package com.gov.sg.baby_bonus_enrollment.domain.disbursement

import java.util.UUID

interface DisbursementRepository {
    fun save(disbursement: Disbursement): Disbursement
    fun findById(id: UUID): Disbursement?
}
