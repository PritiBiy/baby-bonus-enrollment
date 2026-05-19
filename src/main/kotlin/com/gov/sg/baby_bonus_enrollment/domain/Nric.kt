package com.gov.sg.baby_bonus_enrollment.domain

@JvmInline
value class Nric(val value: String) {
    fun masked(): String = value.take(4) + "****" + value.last()
    override fun toString(): String = masked()
}
