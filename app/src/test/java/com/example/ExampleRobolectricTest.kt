package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ItemCategory
import com.example.data.model.ItemType
import com.example.data.remote.RuleBasedFallbackExtractor
import com.example.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LifeVault", appName)
    }

    @Test
    fun `test electricity bill extraction`() {
        val input = "Your electricity bill of ৳1,850 must be paid by September 2."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals("PAYMENT", result.type)
        assertEquals("FINANCE", result.category)
        assertEquals(1850.0, result.amount)
        assertEquals("৳", result.currency)
        assertNotNull(result.date)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test
    fun `test physics project submission deadline`() {
        val input = "All students must submit the physics project by Friday."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals("DEADLINE", result.type)
        assertEquals("EDUCATION", result.category)
        assertEquals("Friday", result.date)
    }

    @Test
    fun `test dentist appointment extraction`() {
        val input = "Your dentist appointment is on September 4 at 3:30 PM."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals("APPOINTMENT", result.type)
        assertEquals("HEALTH", result.category)
        assertNotNull(result.time)
    }

    @Test
    fun `test subscription renewal extraction`() {
        val input = "Your annual subscription will renew automatically for $49.99 on September 15."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals("SUBSCRIPTION", result.type)
        assertEquals("YEARLY", result.subscriptionInterval)
        assertEquals(49.99, result.amount)
    }

    @Test
    fun `test samsung ssd purchase receipt with warranty`() {
        val input = "Samsung SSD ৳8,500 purchased on August 26."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertEquals("WARRANTY", result.type)
        assertEquals(8500.0, result.amount)
        assertNotNull(result.warrantyExpiryDate)
        assertEquals(7, result.returnWindowDays)
    }

    @Test
    fun `test ambiguous date extraction`() {
        val input = "Please send the documents sometime next week."
        val result = RuleBasedFallbackExtractor.extract(input)

        assertTrue(result.confidence < 0.85f)
        assertEquals("Next week", result.date)
    }
}
