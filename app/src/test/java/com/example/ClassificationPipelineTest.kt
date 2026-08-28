package com.example

import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.remote.AIResultValidator
import com.example.data.remote.RuleBasedFallbackExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClassificationPipelineTest {

    @Test
    fun testShortGreetingAndSelfIntro_ClassifiedAsInformationalPersonalNote() {
        val input = "This is Alex"
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.PERSONAL_NOTE.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertTrue(result.action.isBlank())
        assertNull(result.date)
        assertNull(result.amount)
    }

    @Test
    fun testEnglishModifiersGrammarDocument_ClassifiedAsInformationalEducational() {
        val input = """
            Chapter 4: English Modifiers
            A modifier is a word, phrase, or clause that clarifies, qualifies, or limits the meaning of another word in a sentence.
            Pre-modifiers appear before the head noun, while post-modifiers appear after.
            Examples: 'The red car', 'The student sitting in the front row'.
        """.trimIndent()

        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.EDUCATIONAL_CONTENT.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNull(result.amount)
        assertNull(result.subscription)
    }

    @Test
    fun testNewsArticle_ClassifiedAsInformationalNews() {
        val input = """
            Scientists Discovered New Water Sources on Mars
            According to reports published today in the Science Journal, researchers found substantial sub-surface ice reservoirs.
            The findings were announced during the planetary science symposium.
        """.trimIndent()

        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.NEWS_ARTICLE.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNull(result.amount)
        assertNull(result.product)
    }

    @Test
    fun testElectricityBill_ClassifiedAsBillAndActionable() {
        val input = "Your electricity bill of ৳1,850 is due September 2."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.BILL.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        assertEquals(1850.0, result.amount ?: 0.0, 0.01)
        assertEquals("৳", result.currency)
        assertEquals("September 2, 2026", result.date)
        assertTrue(result.action.contains("Pay"))
    }

    @Test
    fun testSubscriptionRenewal_ClassifiedAsSubscriptionAndActionable() {
        val input = "Your Netflix subscription renews automatically on September 15 for $19.99."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.SUBSCRIPTION.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        assertEquals(19.99, result.amount ?: 0.0, 0.01)
        assertEquals("$", result.currency)
    }

    @Test
    fun testDentistAppointment_ClassifiedAsAppointmentAndActionable() {
        val input = "Your dentist appointment is scheduled for September 4 at 3 PM."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.APPOINTMENT.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        assertEquals("September 4, 2026", result.date)
        assertEquals("3 PM", result.time)
    }

    @Test
    fun testValidatorDowngradesFabricatedActionsOnInformationalText() {
        val rawCandidate = com.example.data.model.AIAnalysisResult(
            contentType = ContentType.RECEIPT.name,
            actionability = Actionability.ACTIONABLE.name,
            contentTypeConfidence = 0.5f,
            actionabilityConfidence = 0.4f,
            extractionConfidence = 0.0f,
            title = "Article About Space",
            summary = "Space article",
            action = "Keep warranty",
            amount = 999.0,
            currency = "$",
            date = "Tomorrow"
        )
        val sourceText = "Astronomers observe distant galaxy with James Webb telescope."
        val validationOutcome = AIResultValidator.validate(rawCandidate, sourceText)

        assertEquals(ContentType.GENERAL_INFORMATION.name, validationOutcome.validatedResult.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, validationOutcome.validatedResult.actionability)
        assertFalse(validationOutcome.validatedResult.isActionable)
        assertNull(validationOutcome.validatedResult.amount)
        assertNull(validationOutcome.validatedResult.date)
    }
}
