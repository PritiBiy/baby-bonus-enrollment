package com.gov.sg.baby_bonus_enrollment.external.ica

interface IcaClient {
    fun findChild(nric: String): ChildRecord?
}
