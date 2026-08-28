package com.example

import com.example.data.model.Actionability
import com.example.data.model.Bill
import com.example.data.model.BillStatus
import com.example.data.model.BillType
import com.example.data.model.CardItem
import com.example.data.model.CardNetwork
import com.example.data.model.CardType
import com.example.data.model.ContentType
import com.example.data.remote.RuleBasedFallbackExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BillAndWalletTests {

    @Test
    fun testCase1_GasBillWithDueDate_ExtractedCorrectly() {
        val input = "Gas bill ৳850 due September 5."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.BILL.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        assertEquals(850.0, result.amount ?: 0.0, 0.01)
        assertEquals("৳", result.currency)
        assertEquals(BillType.GAS.name, result.billType)
        assertNotNull(result.date)
        assertTrue(result.date?.contains("September 5") == true)
    }

    @Test
    fun testCase2_GasBillWithoutDueDate_ExtractedWithNullDueDate() {
        val input = "Gas bill ৳750."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.BILL.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        assertEquals(750.0, result.amount ?: 0.0, 0.01)
        assertEquals("৳", result.currency)
        assertEquals(BillType.GAS.name, result.billType)
        assertNull(result.date) // Never invent dates!
    }

    @Test
    fun testCase3_MultiLineBill_ExtractsTotalDueOverPreviousBalance() {
        val input = """
            DESCO Electricity Bill
            Customer ID: 1083920
            Previous Balance: ৳500
            Current Charges: ৳1,000
            Late Fee: ৳100
            Total Due: ৳1,600
            Due Date: September 2
        """.trimIndent()

        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.BILL.name, result.contentType)
        assertEquals(Actionability.ACTIONABLE.name, result.actionability)
        assertTrue(result.isActionable)
        // Must extract 1600 as the true amount due!
        assertEquals(1600.0, result.amount ?: 0.0, 0.01)
        assertEquals(BillType.ELECTRICITY.name, result.billType)
        assertEquals("DESCO", result.billProvider)
        assertNotNull(result.date)
        assertTrue(result.date?.contains("September 2") == true)
    }

    @Test
    fun testCase4_EducationalArticleAboutBills_ClassifiedAsInformational() {
        val input = """
            What is a bill?
            A utility bill is defined as an official statement of money owed for goods or services supplied, such as electricity, gas, water, or internet.
            Consumers must pay attention to billing cycles and meter readings.
        """.trimIndent()

        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.EDUCATIONAL_CONTENT.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNull(result.amount)
    }

    @Test
    fun testCase5_CardInformationExtraction_ClassifiedAsCardInformational() {
        val input = "My Visa card ending in 4821"
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals(ContentType.CARD.name, result.contentType)
        assertEquals(Actionability.INFORMATIONAL.name, result.actionability)
        assertFalse(result.isActionable)
        assertNull(result.amount)
    }

    @Test
    fun testCase6_CardModelStoresOnlySafeMetadata() {
        val card = CardItem(
            id = UUID.randomUUID().toString(),
            cardName = "Salary Visa",
            cardType = CardType.DEBIT.name,
            network = CardNetwork.VISA.name,
            last4Digits = "4821",
            bankIssuer = "City Bank",
            cardholderName = "Alex Turner",
            expiryMonth = 8,
            expiryYear = 2028,
            colorHex = "#1E293B",
            isDefault = true
        )

        assertEquals("4821", card.last4Digits)
        assertEquals(4, card.last4Digits.length)
        assertEquals("Salary Visa", card.nickname)
        assertEquals("Salary Visa", card.cardName)
        assertTrue(card.isDefault)
    }

    @Test
    fun testCase7_BillStatusLifecycle() {
        val bill = Bill(
            id = "bill-001",
            billType = BillType.GAS.name,
            provider = "Titas Gas",
            amountDue = 850.0,
            currency = "BDT",
            dueDate = "September 5, 2026",
            status = BillStatus.UNPAID.name
        )

        assertEquals(BillStatus.UNPAID.name, bill.status)
        assertEquals(850.0, bill.amountDue, 0.01)

        val paidBill = bill.copy(
            status = BillStatus.PAID.name,
            paymentDate = "September 4, 2026"
        )

        assertEquals(BillStatus.PAID.name, paidBill.status)
        assertEquals("September 4, 2026", paidBill.paymentDate)
    }
}
