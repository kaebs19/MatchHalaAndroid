package com.chathala.hala.core.network

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class NetworkResultTest {
    @Test fun `cancelled searches propagate cancellation instead of reporting a network error`() {
        val cancellation = CancellationException("query replaced")
        try {
            safeApiCall<Unit> { throw cancellation }
            fail("Cancellation was swallowed")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }
}
