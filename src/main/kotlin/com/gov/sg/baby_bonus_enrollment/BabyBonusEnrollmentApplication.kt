package com.gov.sg.baby_bonus_enrollment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
class BabyBonusEnrollmentApplication {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
	runApplication<BabyBonusEnrollmentApplication>(*args)
}
