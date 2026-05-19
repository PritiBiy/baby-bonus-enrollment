package com.gov.sg.baby_bonus_enrollment.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NricTest {

    @Test
    fun `masked returns first 4 chars, 4 stars, and last char`() {
        Nric("T2400001A").masked() shouldBe "T240****A"
    }

    @Test
    fun `toString returns masked form`() {
        Nric("S8001234A").toString() shouldBe "S800****A"
    }

    @Test
    fun `two NRICs with the same value are equal`() {
        Nric("T2400001A") shouldBe Nric("T2400001A")
    }
}
