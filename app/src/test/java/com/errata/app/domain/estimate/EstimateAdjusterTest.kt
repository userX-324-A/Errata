package com.errata.app.domain.estimate

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimateAdjusterTest {

    @Test
    fun same_unchanged() {
        assertEquals(20, EstimateAdjuster.adjust(20, EstimateHonesty.SAME))
    }

    @Test
    fun shorter_quarterLessFloorOne() {
        assertEquals(15, EstimateAdjuster.adjust(20, EstimateHonesty.SHORTER))
        assertEquals(1, EstimateAdjuster.adjust(1, EstimateHonesty.SHORTER))
        assertEquals(1, EstimateAdjuster.adjust(2, EstimateHonesty.SHORTER))
        assertEquals(3, EstimateAdjuster.adjust(4, EstimateHonesty.SHORTER))
    }

    @Test
    fun longer_atLeastPlusFive() {
        assertEquals(10, EstimateAdjuster.adjust(5, EstimateHonesty.LONGER))
        assertEquals(25, EstimateAdjuster.adjust(20, EstimateHonesty.LONGER))
    }

    @Test
    fun longer_clampsAtEightHours() {
        assertEquals(
            EstimateAdjuster.MAX_ESTIMATE_MINUTES,
            EstimateAdjuster.adjust(470, EstimateHonesty.LONGER),
        )
        assertEquals(
            EstimateAdjuster.MAX_ESTIMATE_MINUTES,
            EstimateAdjuster.adjust(480, EstimateHonesty.LONGER),
        )
    }

    @Test
    fun zeroOrNegativeTreatedAsOne() {
        assertEquals(1, EstimateAdjuster.adjust(0, EstimateHonesty.SAME))
        assertEquals(1, EstimateAdjuster.adjust(-5, EstimateHonesty.SHORTER))
    }
}
