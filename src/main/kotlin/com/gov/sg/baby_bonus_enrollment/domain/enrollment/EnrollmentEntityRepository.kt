package com.gov.sg.baby_bonus_enrollment.domain.enrollment

import java.util.UUID

interface EnrollmentEntityRepository {
    fun save(enrollment: Enrollment): Enrollment
    fun findById(id: UUID): Enrollment?
    fun findByChildNric(childNric: String): List<Enrollment>
}
