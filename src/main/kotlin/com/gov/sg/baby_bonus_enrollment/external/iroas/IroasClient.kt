package com.gov.sg.baby_bonus_enrollment.external.iroas

interface IroasClient {
    fun findParent(nric: String): ParentRecord?
}
