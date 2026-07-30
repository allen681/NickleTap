package com.nickeltap.clover;

final class NickelRounding {
    enum Mode { NEAREST, UP, DOWN }

    private NickelRounding() {}

    static int adjustment(long totalCents, Mode mode) {
        if (totalCents < 0) throw new IllegalArgumentException("Order total cannot be negative.");
        int remainder = (int) (totalCents % 5L);
        if (remainder == 0) return 0;
        if (mode == Mode.UP) return 5 - remainder;
        if (mode == Mode.DOWN) return -remainder;
        return remainder <= 2 ? -remainder : 5 - remainder;
    }

    static long roundedTotal(long totalCents, Mode mode) {
        return totalCents + adjustment(totalCents, mode);
    }
}
