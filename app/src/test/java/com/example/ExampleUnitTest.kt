package com.example

import com.example.core.CalculatorEngine
import com.example.data.local.AccountEntity
import com.example.data.local.LoanEntity
import com.example.data.model.AccountType
import com.example.data.model.LoanType
import com.example.data.model.NetWorthSummary
import com.example.localization.AppLanguage
import com.example.localization.BdtFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun bdtFormatter_formatsEnglishAndBanglaCorrectly() {
        assertEquals("BDT 1,25,000", BdtFormatter.format(125000.0, AppLanguage.EN))
        assertEquals("BDT ১,২৫,০০০", BdtFormatter.format(125000.0, AppLanguage.BN))
        assertEquals("-BDT 450", BdtFormatter.format(-450.0, AppLanguage.EN))
    }

    @Test
    fun netWorthSummary_computesAssetsMinusLiabilitiesAndRatio() {
        val summary = NetWorthSummary(totalAssets = 90000.0, totalLiabilities = 10000.0)
        assertEquals(80000.0, summary.netWorth, 0.01)
        assertEquals(0.9f, summary.assetRatio, 0.01f)
    }

    @Test
    fun creditCardUtilization_calculatesExpectedPercentage() {
        val card = AccountEntity(
            name = "BRAC Platinum",
            type = AccountType.CREDIT_CARD,
            balance = 25000.0,
            creditLimit = 100000.0,
            bankOrProvider = "BRAC Bank",
            colorArgb = 0xFF0072BCL
        )
        assertEquals(25, card.utilizationPercent)
    }

    @Test
    fun calculatorEngine_evaluatesPrecedenceAndOperatorsCorrectly() {
        assertEquals(690.0, CalculatorEngine.evaluate("450+120×2")!!, 0.001)
        assertEquals(150.5, CalculatorEngine.evaluate("301÷2")!!, 0.001)
        assertEquals(85.0, CalculatorEngine.evaluate("100-30+15")!!, 0.001)
        assertNull(CalculatorEngine.evaluate("50÷0"))
        assertEquals("690", CalculatorEngine.formatResult(690.0))
        assertEquals("150.5", CalculatorEngine.formatResult(150.5))
    }

    @Test
    fun loanEntity_computesRemainingAndSettledState() {
        val partialLoan = LoanEntity(
            personName = "Rafiq",
            loanType = LoanType.OWED_TO_ME,
            totalAmount = 5000.0,
            paidAmount = 2000.0
        )
        assertEquals(3000.0, partialLoan.remainingAmount, 0.01)
        assertFalse(partialLoan.isSettled)

        val fullLoan = partialLoan.copy(paidAmount = 5000.0)
        assertEquals(0.0, fullLoan.remainingAmount, 0.01)
        assertTrue(fullLoan.isSettled)
    }
}
