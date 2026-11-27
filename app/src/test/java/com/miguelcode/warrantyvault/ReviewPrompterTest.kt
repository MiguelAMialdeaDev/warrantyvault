package com.miguelcode.warrantyvault

import com.miguelcode.warrantyvault.review.ReviewPrompter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPrompterTest {

    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `asks the first time and again only after 90 days`() {
        assertTrue(ReviewPrompter.shouldAsk(0L, 1_000L))
        assertFalse(ReviewPrompter.shouldAsk(1_000L, 1_000L + 89 * day))
        assertTrue(ReviewPrompter.shouldAsk(1_000L, 1_000L + 90 * day))
    }

    @Test
    fun `the moment is saving a warranty and having at least two`() {
        assertFalse(ReviewPrompter.isSuccessMoment(null, 5)) // primer valor al abrir la app
        assertFalse(ReviewPrompter.isSuccessMoment(0, 1))
        assertTrue(ReviewPrompter.isSuccessMoment(1, 2))
        assertFalse(ReviewPrompter.isSuccessMoment(3, 2)) // borrar no cuenta
    }
}
