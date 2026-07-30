package com.nickeltap.clover;

final class CashRoundingAdjustment {
    enum Kind { NONE, POSITIVE_LINE_ITEM, ORDER_DISCOUNT }

    final Kind kind;
    final int cents;

    private CashRoundingAdjustment(Kind kind, int cents) {
        this.kind = kind;
        this.cents = cents;
    }

    static CashRoundingAdjustment forTotal(long totalCents, NickelRounding.Mode mode) {
        int cents = NickelRounding.adjustment(totalCents, mode);
        Kind kind = cents > 0
            ? Kind.POSITIVE_LINE_ITEM
            : cents < 0 ? Kind.ORDER_DISCOUNT : Kind.NONE;
        return new CashRoundingAdjustment(kind, cents);
    }
}
