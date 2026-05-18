package com.gov.sg.baby_bonus_enrollment

object TestResourceReader {
    fun read(path: String): String =
        TestResourceReader::class.java.classLoader.getResourceAsStream(path)
            ?.bufferedReader()?.readText()
            ?: error("Test resource not found: $path")
}
