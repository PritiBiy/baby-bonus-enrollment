package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "enrollment")
class EnrollmentEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "child_nric", nullable = false)
    val childNric: String,

    @Column(name = "parent_nric", nullable = false)
    val parentNric: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val relationship: Relationship,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EnrollmentStatus,

    @Column
    var reason: String? = null,

    @Column(name = "enrolled_at")
    var enrolledAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
