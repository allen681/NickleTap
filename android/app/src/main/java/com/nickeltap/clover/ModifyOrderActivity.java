package com.nickeltap.clover;

import android.accounts.Account;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v3.inventory.TaxRate;
import com.clover.sdk.v3.order.Discount;
import com.clover.sdk.v3.order.LineItem;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModifyOrderActivity extends Activity {
    private static final String MARKER_PREFIX = "nickeltap:cash-rounding:v1:";
    private static final String ADJUSTMENT_NAME = "Cash rounding";
    private static final int VERIFY_ATTEMPTS = 8;
    private static final long VERIFY_DELAY_MS = 125L;
    private static final int REQUEST_GET_ACCOUNTS = 510;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView title;
    private TextView message;
    private ProgressBar progress;
    private Button retryButton;
    private Button cancelButton;
    private boolean operationRunning;
    private String orderId;
    private OrderConnector orderConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        orderId = getIntent().getStringExtra(Intents.EXTRA_ORDER_ID);
        prepareAndStartRounding();
    }

    private void prepareAndStartRounding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_GET_ACCOUNTS);
            return;
        }
        if (orderConnector == null) {
            Account account = CloverAccount.getAccount(this);
            if (account != null) orderConnector = new OrderConnector(this, account, null);
        }
        startRounding();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_GET_ACCOUNTS) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            prepareAndStartRounding();
        } else {
            showError(
                getString(R.string.account_permission_title),
                getString(R.string.account_permission_detail)
            );
        }
    }

    private void startRounding() {
        if (orderConnector == null) {
            showError("Clover account unavailable", "Sign in to the Clover device, then try again.");
            return;
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            showError("Order unavailable", "Clover did not provide an open order to round.");
            return;
        }
        showBusy();
        executor.execute(() -> {
            try {
                RoundResult result = roundOrder(orderId, RoundingPreferences.mode(this));
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) showSuccess(result);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showError("Cash total not ready", safeMessage(error));
                    }
                });
            }
        });
    }

    private RoundResult roundOrder(String id, NickelRounding.Mode mode) throws Exception {
        Order order = orderConnector.getOrder(id);
        if (order == null || order.getTotal() == null) throw new IllegalStateException("Clover could not read this order.");

        List<LineItem> existingLineItems = roundingLineItems(order.getLineItems());
        List<Discount> existingDiscounts = roundingDiscounts(order.getDiscounts());
        if (existingLineItems.size() + existingDiscounts.size() > 1) {
            throw new IllegalStateException("This order has more than one cash-rounding adjustment. Review it before payment.");
        }
        if (existingLineItems.size() + existingDiscounts.size() == 1) {
            if (order.getTotal() % 5L == 0L) {
                return new RoundResult(true, 0, order.getTotal());
            }
            removeExistingAdjustment(id, existingLineItems, existingDiscounts);
            order = awaitOrderWithoutAdjustment(id);
            if (order == null || order.getTotal() == null) {
                throw new IllegalStateException("Clover could not refresh the previous cash-rounding adjustment.");
            }
        }

        long originalTotal = order.getTotal();
        Long originalTaxTotal = taxTotal(order);
        CashRoundingAdjustment adjustment = CashRoundingAdjustment.forTotal(originalTotal, mode);
        long expectedTotal = originalTotal + adjustment.cents;
        if (expectedTotal < 0) throw new IllegalStateException("The rounded total cannot be negative.");
        if (adjustment.kind == CashRoundingAdjustment.Kind.NONE) {
            return new RoundResult(false, 0, originalTotal);
        }

        String createdId;
        if (adjustment.kind == CashRoundingAdjustment.Kind.POSITIVE_LINE_ITEM) {
            LineItem lineItem = new LineItem();
            lineItem.setName(ADJUSTMENT_NAME);
            lineItem.setPrice((long) adjustment.cents);
            lineItem.setNote(MARKER_PREFIX + mode.name());
            lineItem.setIsRevenue(false);
            LineItem created = orderConnector.addCustomLineItem(id, lineItem, false);
            createdId = created == null ? null : created.getId();
        } else {
            Discount discount = new Discount();
            discount.setName(ADJUSTMENT_NAME);
            discount.setAmount((long) adjustment.cents);
            Discount created = orderConnector.addDiscount2(id, discount);
            createdId = created == null ? null : created.getId();
        }
        if (createdId == null) {
            throw new IllegalStateException("Clover did not create the cash-rounding adjustment.");
        }

        Order updated = awaitOrderTotal(id, expectedTotal);
        Long updatedTaxTotal = taxTotal(updated);
        boolean taxChanged = originalTaxTotal != null
            && updatedTaxTotal != null
            && !originalTaxTotal.equals(updatedTaxTotal);
        if (updated == null || taxChanged) {
            try {
                removeCreatedAdjustment(id, adjustment.kind, createdId);
                if (awaitOrderTotal(id, originalTotal) == null) {
                    throw new IllegalStateException("Clover did not restore the original total.");
                }
            } catch (Exception rollbackError) {
                throw new IllegalStateException("Clover could not verify the new total. Review the order before payment.", rollbackError);
            }
            if (taxChanged) {
                throw new IllegalStateException("Clover changed the calculated tax, so the adjustment was removed.");
            }
            throw new IllegalStateException("Clover did not update the total, so the adjustment was removed.");
        }
        return new RoundResult(false, adjustment.cents, updated.getTotal());
    }

    private void removeExistingAdjustment(
        String orderId,
        List<LineItem> lineItems,
        List<Discount> discounts
    ) throws Exception {
        if (!lineItems.isEmpty()) {
            String id = lineItems.get(0).getId();
            if (id == null) throw new IllegalStateException("Clover could not identify the existing cash-rounding line.");
            orderConnector.deleteLineItems(orderId, Collections.singletonList(id));
            return;
        }
        String id = discounts.get(0).getId();
        if (id == null) throw new IllegalStateException("Clover could not identify the existing cash-rounding discount.");
        orderConnector.deleteDiscounts(orderId, Collections.singletonList(id));
    }

    private void removeCreatedAdjustment(
        String orderId,
        CashRoundingAdjustment.Kind kind,
        String adjustmentId
    ) throws Exception {
        if (kind == CashRoundingAdjustment.Kind.POSITIVE_LINE_ITEM) {
            orderConnector.deleteLineItems(orderId, Collections.singletonList(adjustmentId));
        } else {
            orderConnector.deleteDiscounts(orderId, Collections.singletonList(adjustmentId));
        }
    }

    private Order awaitOrderTotal(String orderId, long expectedTotal) throws Exception {
        for (int attempt = 0; attempt < VERIFY_ATTEMPTS; attempt++) {
            Order order = orderConnector.getOrder(orderId);
            if (order != null && order.getTotal() != null && order.getTotal() == expectedTotal) return order;
            if (attempt + 1 < VERIFY_ATTEMPTS) SystemClock.sleep(VERIFY_DELAY_MS);
        }
        return null;
    }

    private Order awaitOrderWithoutAdjustment(String orderId) throws Exception {
        for (int attempt = 0; attempt < VERIFY_ATTEMPTS; attempt++) {
            Order order = orderConnector.getOrder(orderId);
            if (order != null
                && order.getTotal() != null
                && roundingLineItems(order.getLineItems()).isEmpty()
                && roundingDiscounts(order.getDiscounts()).isEmpty()) {
                return order;
            }
            if (attempt + 1 < VERIFY_ATTEMPTS) SystemClock.sleep(VERIFY_DELAY_MS);
        }
        return null;
    }

    private List<LineItem> roundingLineItems(List<LineItem> lineItems) {
        if (lineItems == null) return Collections.emptyList();
        java.util.ArrayList<LineItem> matches = new java.util.ArrayList<>();
        for (LineItem item : lineItems) {
            if (item != null && item.getNote() != null && item.getNote().startsWith(MARKER_PREFIX)) {
                matches.add(item);
            }
        }
        return matches;
    }

    private Long taxTotal(Order order) {
        if (order == null || order.getLineItems() == null) return null;
        long total = 0L;
        for (LineItem lineItem : order.getLineItems()) {
            if (lineItem == null || lineItem.getTaxRates() == null) return null;
            for (TaxRate taxRate : lineItem.getTaxRates()) {
                if (taxRate == null) continue;
                if (taxRate.getTaxAmount() == null) return null;
                total += taxRate.getTaxAmount();
            }
        }
        return total;
    }

    private List<Discount> roundingDiscounts(List<Discount> discounts) {
        if (discounts == null) return Collections.emptyList();
        java.util.ArrayList<Discount> matches = new java.util.ArrayList<>();
        for (Discount discount : discounts) {
            if (discount == null || !ADJUSTMENT_NAME.equals(discount.getName())) continue;
            Long amount = discount.getAmount();
            if (amount != null
                && amount >= -4L
                && amount <= -1L
                && discount.getPercentage() == null
                && discount.getPercentageDecimal() == null
                && discount.getDiscount() == null) {
                matches.add(discount);
            }
        }
        return matches;
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(30), dp(28), dp(30));
        root.setBackgroundColor(Color.rgb(244, 241, 233));

        TextView badge = text(getString(R.string.coin_badge), 18, true, Color.rgb(18, 61, 49));
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(Color.rgb(31, 199, 122));
        root.addView(badge, new LinearLayout.LayoutParams(dp(56), dp(56)));

        title = text(getString(R.string.rounding_title), 25, true, Color.rgb(23, 39, 35));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = wrap();
        titleParams.topMargin = dp(20);
        root.addView(title, titleParams);

        message = text(getString(R.string.rounding_message), 14, false, Color.rgb(93, 107, 101));
        message.setGravity(Gravity.CENTER);
        message.setMaxWidth(dp(520));
        LinearLayout.LayoutParams messageParams = wrap();
        messageParams.topMargin = dp(9);
        root.addView(message, messageParams);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        progressParams.topMargin = dp(24);
        root.addView(progress, progressParams);

        retryButton = new Button(this);
        retryButton.setAllCaps(false);
        retryButton.setText(R.string.try_again);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(view -> prepareAndStartRounding());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(dp(230), dp(52));
        retryParams.topMargin = dp(22);
        root.addView(retryButton, retryParams);

        cancelButton = new Button(this);
        cancelButton.setAllCaps(false);
        cancelButton.setText(R.string.return_without_rounding);
        cancelButton.setOnClickListener(view -> cancelAndFinish());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(dp(230), dp(48));
        cancelParams.topMargin = dp(8);
        root.addView(cancelButton, cancelParams);
        return root;
    }

    private void showBusy() {
        operationRunning = true;
        title.setText(R.string.rounding_title);
        message.setText(R.string.rounding_message);
        progress.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        cancelButton.setEnabled(false);
    }

    private void showSuccess(RoundResult result) {
        progress.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        if (result.adjustmentCents == 0) {
            title.setText(result.idempotent ? R.string.already_rounded : R.string.already_on_nickel);
            message.setText(R.string.no_adjustment_needed);
        } else {
            title.setText(R.string.cash_total_ready);
            String direction = getString(result.adjustmentCents > 0 ? R.string.direction_up : R.string.direction_down);
            int absoluteAdjustment = Math.abs(result.adjustmentCents);
            message.setText(getResources().getQuantityString(
                R.plurals.rounded_result,
                absoluteAdjustment,
                direction,
                absoluteAdjustment,
                formatMoney(result.roundedTotalCents)
            ));
        }
        handler.postDelayed(this::completeAndFinish, 700);
    }

    private void showError(String heading, String detail) {
        operationRunning = false;
        title.setText(heading);
        message.setText(detail);
        progress.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        cancelButton.setEnabled(true);
    }

    private String formatMoney(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(cents / 100.0);
    }

    private void completeAndFinish() {
        operationRunning = false;
        setResult(RESULT_OK, new Intent());
        finish();
    }

    private void cancelAndFinish() {
        if (operationRunning) return;
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (!operationRunning) super.onBackPressed();
    }

    private String safeMessage(Exception error) {
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? "Review the order and try again." : value;
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        if (orderConnector != null) orderConnector.disconnect();
        super.onDestroy();
    }

    private static final class RoundResult {
        final boolean idempotent;
        final int adjustmentCents;
        final long roundedTotalCents;

        RoundResult(boolean idempotent, int adjustmentCents, long roundedTotalCents) {
            this.idempotent = idempotent;
            this.adjustmentCents = adjustmentCents;
            this.roundedTotalCents = roundedTotalCents;
        }
    }
}
