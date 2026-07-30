# NickelTap for Clover

NickelTap is a local Android app for Clover POS. It adds a **Round cash** action to Clover's standard payment screen. The cashier taps it before selecting Cash, and NickelTap moves the open order total to a five-cent amount.

There is no donation feature, merchant dashboard, OAuth flow, register pairing, remote database, or transaction upload.

## Checkout behavior

The merchant chooses one rule on each Clover device:

- **Nearest nickel:** remainders of 1¢ and 2¢ round down; 3¢ and 4¢ round up.
- **Always round down:** moves to the previous five-cent amount.
- **Always round up:** moves to the next five-cent amount.
- Totals already ending in 0¢ or 5¢ are unchanged.

At checkout:

1. The cashier taps **Round cash** on Clover's merchant-facing payment screen.
2. Clover launches NickelTap with the open order ID.
3. NickelTap reads the total and checks that the order has not already been rounded.
4. It adds a non-taxable, non-revenue custom line item when rounding up, or a fixed-amount order discount when rounding down. Clover rejects negative custom line-item prices, so downward adjustments must use its supported discount model.
5. It reads the order again and verifies the exact expected total.
6. On verification failure, it attempts to remove the adjustment and leaves the cashier on an error screen.
7. On success, it returns RESULT_OK to Clover checkout.

The line-item marker makes a second tap on the same order idempotent.

> Clover's public Android integration exposes order modification as a cashier-tapped action. It does not provide a safe way for this app to intercept only the built-in Cash button. The cashier must tap **Round cash** only for a cash sale.

## Project structure

- android/ — the functional Clover Android app.
- app/ — a static product, setup, privacy, and terms site for App Market/support URLs.
- tests/ — static-site bundle checks.

The Android app has no INTERNET permission. It requests Android's GET_ACCOUNTS permission only so CloverAccount can locate the Clover merchant account required by Order Connector, including at runtime on Android 6.0 and newer. Its only local state is the selected rounding rule in private SharedPreferences.

## Build the Android app

Requirements:

- JDK 17
- Android SDK Platform 35 and Build Tools 35
- Gradle 8.9, or the included Gradle wrapper after it has been generated

From the repository root:

    cd android
    ./gradlew.bat testDebugUnitTest lintSandbox assembleSandbox
    powershell -ExecutionPolicy Bypass -File scripts/sign-sandbox-apk.ps1

The `sandbox` variant is a non-debuggable, release-mode package. The signing script creates and reuses a dedicated RSA sandbox identity, then enables only the V1/JAR signature scheme because Clover rejects debug certificates and APKs containing Android V2, V3, or V4 signatures.

The Clover sandbox APK is emitted at:

    android/app/build/outputs/apk/sandbox/NickelTap-sandbox-release-v2.apk

Back up `android/.sandbox-signing/nickeltap-sandbox.jks` and its password file together; Clover requires every later APK for this sandbox app to use the same key. Both files are ignored by Git. Use this package and identity only with the sandbox app. Generate and protect a separate permanent production signing key for production.

## Create the Clover app

In Clover's Global Developer Dashboard:

1. Create an app and select **Android app** as the platform.
2. Use Android package name com.nickeltap.clover.
3. Request **Read orders** and **Write orders** permissions.
4. Select the Clover devices and merchant service plans you intend to support.
5. Upload the signed release APK under **App Releases**.
6. Install it on a sandbox test merchant before any production submission.

No REST configuration, OAuth callback, CORS domain, webhook, or web-app launch URL is needed for the Android app.

The manifest registers:

    <action android:name="clover.intent.action.MODIFY_ORDER" />

Clover uses that action and the activity label Round cash to add NickelTap to its payment screen.

The app targets SDK 25 to preserve Clover account discovery, as Clover currently recommends. Its minimum SDK is 17 so it can be offered to the full Clover device range; device availability should still be limited to hardware actually covered by the validation matrix.

## Required device validation

The pure rounding function has JVM tests for all cent endings and all three rules. Before production, validate on every supported Clover device and checkout app version:

- totals ending in every digit from 0 through 9;
- nearest, always-down, and always-up rules;
- a total already divisible by five;
- a second tap on the same open order;
- cancellation and retry;
- receipt and order-history display;
- taxes remaining unchanged;
- offline operation;
- split tenders, tips, refunds, voids, and reopened orders;
- employee permissions and restricted accounts;
- accidental use before a non-cash tender.

The APK cannot be considered production-validated until these flows pass on a Clover sandbox device or Clover Android emulator with the relevant Clover apps installed.

## Static support site

The web portion is documentation only. It has no login, API routes, database, analytics, or Clover connection.

    npm install
    npm run build
    npm test

Before an App Market submission, replace the support-page placeholder with the deploying operator's legal identity and monitored support email, then review the privacy policy and terms with qualified counsel.

## Key official references

- [Create a new Clover app](https://docs.clover.com/dev/docs/gdp-create-new-app)
- [Clover action intents](https://docs.clover.com/dev/docs/intents-and-broadcasts)
- [Use the Order Connector](https://docs.clover.com/dev/docs/using-order-connector)
- [Manage Android app releases](https://docs.clover.com/dev/docs/gdp-manage-android-app-releases)
- [Clover Android SDK](https://github.com/clover/clover-android-sdk)
