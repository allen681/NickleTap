package com.nickeltap.clover;

import android.content.Context;
import android.content.SharedPreferences;

final class RoundingPreferences {
    private static final String PREFERENCES = "nickeltap_settings";
    private static final String MODE = "rounding_mode";

    private RoundingPreferences() {}

    static NickelRounding.Mode mode(Context context) {
        String stored = preferences(context).getString(MODE, NickelRounding.Mode.NEAREST.name());
        try {
            return NickelRounding.Mode.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return NickelRounding.Mode.NEAREST;
        }
    }

    static void saveMode(Context context, NickelRounding.Mode mode) {
        preferences(context).edit().putString(MODE, mode.name()).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
}
