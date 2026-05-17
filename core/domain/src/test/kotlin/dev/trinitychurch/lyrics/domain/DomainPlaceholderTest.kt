package dev.trinitychurch.lyrics.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DomainPlaceholderTest : StringSpec({
    "domain layer placeholder: 1 + 1 should equal 2" {
        (1 + 1) shouldBe 2
    }
})
