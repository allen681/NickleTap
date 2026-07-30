import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Setup and support" };

export default function SupportPage() {
  return (
    <main className="legal-page">
      <nav className="legal-nav" aria-label="Support navigation">
        <Link className="brand" href="/"><span className="coin">5¢</span><span>NickelTap</span></Link>
        <Link href="/">Back to NickelTap</Link>
      </nav>
      <article>
        <p className="eyebrow">Setup and support</p>
        <h1>Install it once. Tap it for cash.</h1>
        <p className="legal-intro">NickelTap runs entirely on the Clover device. There is no web account to connect and no register-pairing code.</p>

        <h2>Merchant setup</h2>
        <ol className="support-steps">
          <li>Install NickelTap on the Clover test merchant or device.</li>
          <li>Open NickelTap from the app launcher.</li>
          <li>Select Nearest nickel, Always round down, or Always round up, then save.</li>
          <li>Create a test order and proceed to Clover&apos;s payment screen.</li>
          <li>For a cash sale, tap Round cash, verify the adjusted total, then select Cash.</li>
        </ol>

        <h2>If Round cash is missing</h2>
        <p>Confirm that NickelTap is installed for the same merchant, that the APK declares Clover&apos;s MODIFY_ORDER action, and that the app has Read orders and Write orders permissions.</p>

        <h2>Required sandbox tests</h2>
        <p>Test totals ending in every digit from 0 through 9, each rounding rule, a second tap on the same order, cancellation, split tender, refunds, offline operation, and an accidental non-cash workflow. Confirm receipts and reports with the merchant&apos;s accountant before production use.</p>

        <h2>Contact</h2>
        <p>The deploying operator must add a monitored support email to the Clover App Market listing before distributing NickelTap to merchants.</p>

        <p className="legal-note">Do not send card data, Clover credentials, customer payment information, or sensitive receipt details in a support request.</p>
      </article>
    </main>
  );
}
