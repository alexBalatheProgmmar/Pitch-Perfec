package com.example

import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemType
import com.example.data.remote.AIResultValidator
import com.example.data.remote.ImageContentClassifier
import com.example.data.remote.RuleBasedFallbackExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClassificationPipelineTest {

    // Test 1: Normal photo of a person
    @Test
    fun test1_PersonPortraitPhotograph_ClassifiedAsPhotographNotReceipt() {
        val input = "Portrait photo of a person smiling outdoors in a park"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.PHOTOGRAPH.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
        assertNull(result.amount)
        assertNull(result.merchant)
    }

    // Test 2: Landscape / nature photo
    @Test
    fun test2_MountainSunsetLandscape_ClassifiedAsPhotographNotReceipt() {
        val input = "Beautiful mountain landscape during sunset with trees and lake"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.PHOTOGRAPH.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
        assertNull(result.amount)
    }

    // Test 3: Dog / pet photo
    @Test
    fun test3_GoldenRetrieverDog_ClassifiedAsPhotographNotReceipt() {
        val input = "Golden retriever puppy sitting in the grass looking at camera"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.PHOTOGRAPH.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertEquals("Golden Retriever", result.title)
        assertNotEquals("Store Purchase Receipt", result.title)
        assertNull(result.amount)
    }

    // Test 4: Textbook page
    @Test
    fun test4_TextbookPageModifiers_ClassifiedAsEducationalNotReceipt() {
        val input = """
            Chapter 4: English Modifiers
            A modifier is a word, phrase, or clause that clarifies, qualifies, or limits the meaning of another word in a sentence.
            Pre-modifiers appear before the head noun, while post-modifiers appear after.
            Examples: 'The red car', 'The student sitting in the front row'.
        """.trimIndent()

        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.EDUCATIONAL_PAGE.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
        assertNull(result.amount)
    }

    // Test 5: News screenshot
    @Test
    fun test5_NewsArticle_ClassifiedAsNewsNotReceipt() {
        val input = """
            Scientists Discovered New Water Sources on Mars
            According to reports published today in the Science Journal, researchers found substantial sub-surface ice reservoirs.
            The findings were announced during the planetary science symposium.
        """.trimIndent()

        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.NEWS.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
        assertNull(result.amount)
    }

    // Test 6: Chat screenshot
    @Test
    fun test6_ChatScreenshot_ClassifiedAsChatNotReceipt() {
        val input = "WhatsApp chat screenshot conversation between Alice and Bob yesterday at 4:15 pm"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.CHAT_SCREENSHOT.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
        assertNull(result.amount)
    }

    // Test 7: Product photo
    @Test
    fun test7_ProductImageSamsungSSD_ClassifiedAsProductNotReceipt() {
        val input = "Product photo of Samsung SSD T7 Portable Solid State Drive"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.PRODUCT_IMAGE.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertEquals("Samsung SSD", result.title)
        assertNotEquals("Store Purchase Receipt", result.title)
    }

    // Test 8: Academic math problem
    @Test
    fun test8_AcademicMathProblem_ClassifiedAsEducationalNotReceipt() {
        val input = "Calculus homework: Solve quadratic formula equation for x, finding derivative and integral f(x) = ax^2 + bx + c"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.EDUCATIONAL_PAGE.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
    }

    // Test 9: Real store purchase receipt
    @Test
    fun test9_RealStoreReceipt_ClassifiedAsReceiptBranch() {
        val input = """
            Walmart Supercenter Store #1234
            Cashier: Jane
            Items:
            1x Milk $3.50
            1x Bread $2.00
            Subtotal: $5.50
            Tax: $0.44
            Total: $5.94
            Receipt #987654
        """.trimIndent()

        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.RECEIPT.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertEquals(5.94, result.amount ?: 0.0, 0.01)
    }

    // Test 10: Bill with amount and deadline
    @Test
    fun test10_ElectricityBill_ClassifiedAsBillAndActionable() {
        val input = "DESCO Electricity Bill. Total Due: ৳1,850. Due date: September 2, 2026."
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.BILL.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        assertEquals(1850.0, result.amount ?: 0.0, 0.01)
        assertEquals("৳", result.currency)
    }

    // Test 11: Random image
    @Test
    fun test11_RandomImage_ClassifiedAsInformationalNotReceipt() {
        val input = "Random abstract geometric pattern wallpaper"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertNotEquals(ContentType.RECEIPT.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNotEquals("Store Purchase Receipt", result.title)
    }

    // Test 12: Blurry / unreadable image
    @Test
    fun test12_BlurryImage_ClassifiedAsUnknownNotReceipt() {
        val input = "Blurry unreadable dark image with camera motion noise"
        val classification = ImageContentClassifier.classifyFromContextOrHint(input)
        val result = ImageContentClassifier.routeAndAnalyze(classification, input, null)

        assertEquals(ContentType.UNKNOWN.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertEquals("Unidentified Image", result.title)
        assertNotEquals("Store Purchase Receipt", result.title)
    }

    @Test
    fun testValidatorDowngradesFabricatedActionsOnInformationalText() {
        val rawCandidate = AIAnalysisResult(
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

        assertEquals(ContentType.UNKNOWN.name, validationOutcome.validatedResult.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, validationOutcome.validatedResult.actionability)
        assertFalse(validationOutcome.validatedResult.isActionable)
        assertNull(validationOutcome.validatedResult.amount)
        assertNull(validationOutcome.validatedResult.date)
    }

    // PDF Test 1: Commercial Invoice PDF
    @Test
    fun testPdf1_CommercialInvoice_ClassifiedAsInvoiceNotGeneralInformation() {
        val invoiceText = """
            ACME CORPORATION
            123 Business Way, Suite 100
            Invoice Number: INV-2024-8891
            Issue Date: 2026-08-15
            Due Date: 2026-09-15
            Billed To: Tech Innovations Ltd
            Customer ID: CUST-4421

            Description            Qty    Unit Price    Total
            Cloud Server Hosting     1      $800.00   $800.00
            Database Backup Addon    1      $200.00   $200.00

            Subtotal: $1,000.00
            Tax (10%): $100.00
            Total Amount Due: $1,100.00
            Please remit payment by due date.
        """.trimIndent()

        val pdfData = com.example.util.ExtractedPdfData(
            fileName = "Acme_Invoice_INV8891.pdf",
            fileSize = 102400,
            pageCount = 1,
            fullText = invoiceText,
            pageTexts = listOf(com.example.util.PageTextData(1, invoiceText, 15)),
            isScannedPdf = false,
            contentHash = "hash123"
        )

        val result = com.example.data.remote.DocumentContentClassifier.classifyAndExtract(pdfData)

        assertEquals(ContentType.INVOICE.name, result.contentType)
        assertNotEquals(ContentType.GENERAL_INFORMATION.name, result.contentType)
        assertEquals("INV-2024-8891", result.invoiceNumber)
        assertEquals("ACME CORPORATION", result.organization)
        assertEquals(1100.0, result.amount ?: 0.0, 0.01)
        assertEquals("2026-09-15", result.date)
        assertTrue(result.isActionable)
    }

    // PDF Test 2: Educational English Modifiers PDF
    @Test
    fun testPdf2_EnglishModifiersDocument_ClassifiedAsEducationalNotGeneralInformation() {
        val educationalText = """
            Chapter 4: English Modifiers and Sentence Syntax
            Department of Linguistics, Cambridge University
            
            1. Introduction to Modifiers
            A modifier is an optional word, phrase, or clause that qualifies or limits the meaning of another grammatical element.
            In English syntax, modifiers can be divided into pre-modifiers and post-modifiers.
            
            2. Types of Pre-modifiers:
            - Adjectives: 'The loud thunder'
            - Participles: 'The burning flame'
            - Noun adjuncts: 'History teacher'
            
            3. Exercises:
            Identify the head noun and all associated modifiers in the following paragraph.
        """.trimIndent()

        val pdfData = com.example.util.ExtractedPdfData(
            fileName = "English_Modifiers_Lecture.pdf",
            fileSize = 250000,
            pageCount = 4,
            fullText = educationalText,
            pageTexts = listOf(com.example.util.PageTextData(1, educationalText, 18)),
            isScannedPdf = false,
            contentHash = "hash456"
        )

        val result = com.example.data.remote.DocumentContentClassifier.classifyAndExtract(pdfData)

        assertEquals(ContentType.EDUCATIONAL_DOCUMENT.name, result.contentType)
        assertNotEquals(ContentType.GENERAL_INFORMATION.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNull(result.amount)
    }

    // PDF Test 3: Research Paper PDF
    @Test
    fun testPdf3_ResearchPaper_ClassifiedAsResearchPaper() {
        val paperText = """
            Attention Is All You Need
            Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit, Llion Jones, Aidan N. Gomez, Łukasz Kaiser, Illia Polosukhin
            Google Brain & Google Research
            
            Abstract:
            The dominant sequence transduction models are based on complex recurrent or convolutional neural networks.
            We propose the Transformer, a model architecture eschewing recurrence and relying entirely on attention mechanisms.
            
            1. Introduction
            Recurrent models typically factor computation along the symbol positions of the input and output sequences.
            2. Model Architecture
            Most competitive neural sequence models have an encoder-decoder structure.
            
            References:
            [1] Bahdanau et al., Neural Machine Translation by Jointly Learning to Align and Translate, ICLR 2015.
        """.trimIndent()

        val pdfData = com.example.util.ExtractedPdfData(
            fileName = "Attention_Is_All_You_Need.pdf",
            fileSize = 512000,
            pageCount = 11,
            fullText = paperText,
            pageTexts = listOf(com.example.util.PageTextData(1, paperText, 20)),
            isScannedPdf = false,
            contentHash = "hash789"
        )

        val result = com.example.data.remote.DocumentContentClassifier.classifyAndExtract(pdfData)

        assertEquals(ContentType.RESEARCH_PAPER.name, result.contentType)
        assertNotEquals(ContentType.GENERAL_INFORMATION.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
    }

    // PDF Test 4: Legal Contract PDF
    @Test
    fun testPdf4_LegalContractAgreement_ClassifiedAsContract() {
        val contractText = """
            NON-DISCLOSURE AND CONFIDENTIALITY AGREEMENT
            This Agreement is made and entered into as of October 1, 2026, by and between Alpha Solutions LLC and Beta Corp.
            
            WHEREAS, the Disclosing Party possesses certain proprietary and confidential information;
            NOW, THEREFORE, the parties agree as follows:
            1. Confidential Information definition.
            2. Obligations of Receiving Party.
            3. Term and Termination: This agreement shall remain in effect for 3 years.
            4. Governing Law: The laws of the State of California.
            
            IN WITNESS WHEREOF, the parties hereto have executed this Agreement.
            Signature: ____________________
        """.trimIndent()

        val pdfData = com.example.util.ExtractedPdfData(
            fileName = "NDA_Alpha_Beta.pdf",
            fileSize = 180000,
            pageCount = 3,
            fullText = contractText,
            pageTexts = listOf(com.example.util.PageTextData(1, contractText, 16)),
            isScannedPdf = false,
            contentHash = "hashnda"
        )

        val result = com.example.data.remote.DocumentContentClassifier.classifyAndExtract(pdfData)

        assertEquals(ContentType.CONTRACT.name, result.contentType)
        assertNotEquals(ContentType.GENERAL_INFORMATION.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
    }
}
