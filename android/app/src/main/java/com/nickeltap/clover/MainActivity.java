package com.nickeltap.clover;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private RadioGroup modes;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        selectSavedMode();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244, 241, 233));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(30), dp(34), dp(30), dp(30));
        root.setBackgroundColor(Color.rgb(244, 241, 233));

        TextView badge = text(getString(R.string.coin_badge), 18, true, Color.rgb(18, 61, 49));
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(Color.rgb(31, 199, 122));
        root.addView(badge, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView title = text(getString(R.string.settings_title), 28, true, Color.rgb(23, 39, 35));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(17);
        root.addView(title, titleParams);

        TextView intro = text(getString(R.string.settings_intro), 14, false, Color.rgb(86, 104, 97));
        intro.setGravity(Gravity.CENTER);
        intro.setMaxWidth(dp(580));
        LinearLayout.LayoutParams introParams = matchWrap();
        introParams.topMargin = dp(8);
        root.addView(intro, introParams);

        modes = new RadioGroup(this);
        modes.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(Math.min(dp(520), getResources().getDisplayMetrics().widthPixels - dp(60)), LinearLayout.LayoutParams.WRAP_CONTENT);
        groupParams.topMargin = dp(26);
        root.addView(modes, groupParams);

        modes.addView(option(100, getString(R.string.nearest_title), getString(R.string.nearest_detail)));
        modes.addView(option(101, getString(R.string.down_title), getString(R.string.down_detail)));
        modes.addView(option(102, getString(R.string.up_title), getString(R.string.up_detail)));

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setText(R.string.save_rule);
        save.setOnClickListener(view -> saveMode());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(groupParams.width, dp(54));
        buttonParams.topMargin = dp(19);
        root.addView(save, buttonParams);

        status = text("", 13, true, Color.rgb(18, 99, 69));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(13);
        root.addView(status, statusParams);

        TextView note = text(getString(R.string.local_only_note), 11, false, Color.rgb(102, 115, 110));
        note.setGravity(Gravity.CENTER);
        note.setMaxWidth(dp(580));
        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.topMargin = dp(24);
        root.addView(note, noteParams);
        scroll.addView(root, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scroll;
    }

    private RadioButton option(int id, String title, String detail) {
        RadioButton option = new RadioButton(this);
        option.setId(id);
        option.setText(getString(R.string.option_label, title, detail));
        option.setTextSize(14);
        option.setTextColor(Color.rgb(31, 52, 46));
        option.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(31, 199, 122)));
        }
        return option;
    }

    private void selectSavedMode() {
        NickelRounding.Mode mode = RoundingPreferences.mode(this);
        modes.check(mode == NickelRounding.Mode.DOWN ? 101 : mode == NickelRounding.Mode.UP ? 102 : 100);
        status.setText(getString(R.string.current_rule, displayName(mode)));
    }

    private void saveMode() {
        int checked = modes.getCheckedRadioButtonId();
        NickelRounding.Mode mode = checked == 101
            ? NickelRounding.Mode.DOWN
            : checked == 102 ? NickelRounding.Mode.UP : NickelRounding.Mode.NEAREST;
        RoundingPreferences.saveMode(this, mode);
        status.setText(getString(R.string.saved_rule, displayName(mode)));
    }

    private String displayName(NickelRounding.Mode mode) {
        if (mode == NickelRounding.Mode.DOWN) return getString(R.string.down_title);
        if (mode == NickelRounding.Mode.UP) return getString(R.string.up_title);
        return getString(R.string.nearest_title);
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
