package com.nickeltap.clover;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NickelRoundingTest {
    @Test
    public void nearestRoundsEveryCentEndingCorrectly() {
        int[] actual = new int[10];
        for (int ending = 0; ending < 10; ending++) {
            actual[ending] = NickelRounding.adjustment(1800 + ending, NickelRounding.Mode.NEAREST);
        }
        assertArrayEquals(new int[]{0, -1, -2, 2, 1, 0, -1, -2, 2, 1}, actual);
    }

    @Test
    public void upAlwaysMovesToNextNickel() {
        assertEquals(0, NickelRounding.adjustment(1850, NickelRounding.Mode.UP));
        assertEquals(4, NickelRounding.adjustment(1851, NickelRounding.Mode.UP));
        assertEquals(1, NickelRounding.adjustment(1854, NickelRounding.Mode.UP));
    }

    @Test
    public void downAlwaysMovesToPreviousNickel() {
        assertEquals(0, NickelRounding.adjustment(1850, NickelRounding.Mode.DOWN));
        assertEquals(-1, NickelRounding.adjustment(1851, NickelRounding.Mode.DOWN));
        assertEquals(-4, NickelRounding.adjustment(1854, NickelRounding.Mode.DOWN));
    }

    @Test
    public void positiveAdjustmentUsesNonTaxableLineItem() {
        CashRoundingAdjustment adjustment =
            CashRoundingAdjustment.forTotal(599, NickelRounding.Mode.NEAREST);

        assertEquals(CashRoundingAdjustment.Kind.POSITIVE_LINE_ITEM, adjustment.kind);
        assertEquals(1, adjustment.cents);
    }

    @Test
    public void negativeAdjustmentUsesFixedAmountDiscount() {
        CashRoundingAdjustment adjustment =
            CashRoundingAdjustment.forTotal(801, NickelRounding.Mode.NEAREST);

        assertEquals(CashRoundingAdjustment.Kind.ORDER_DISCOUNT, adjustment.kind);
        assertEquals(-1, adjustment.cents);
    }

    @Test
    public void nickelTotalDoesNotCreateOrderMutation() {
        CashRoundingAdjustment adjustment =
            CashRoundingAdjustment.forTotal(800, NickelRounding.Mode.NEAREST);

        assertEquals(CashRoundingAdjustment.Kind.NONE, adjustment.kind);
        assertEquals(0, adjustment.cents);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeTotalsAreRejected() {
        NickelRounding.adjustment(-1, NickelRounding.Mode.NEAREST);
    }
}
