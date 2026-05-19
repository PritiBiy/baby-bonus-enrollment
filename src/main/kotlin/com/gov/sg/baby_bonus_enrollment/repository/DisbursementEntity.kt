package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "disbursement")
class DisbursementEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "enrollment_id", nullable = false)
    val enrollmentId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: DisbursementType,

    @Column(nullable = false, precision = 12, scale = 2)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DisbursementStatus,

    @Column(name = "processed_at")
    var processedAt: Instant? = null
)
