import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Privacy policy" };

export default function PrivacyPage() {
  return (
    <main className="legal-page">
      <nav className="legal-nav" aria-label="Legal navigation">
        <Link className="brand" href="/"><span className="coin">5¢</span><span>NickelTap</span></Link>
        <Link href="/">Back to NickelTap</Link>
      </nav>
      <article>
        <p className="eyebrow">Last updated July 21, 2026</p>
        <h1>Privacy policy</h1>
        <p className="legal-intro">NickelTap is a local Clover Android utility. It has no developer-operated account, dashboard, analytics service, or remote database.</p>

        <h2>Data used on the Clover device</h2>
        <p>NickelTap locates the Clover merchant account on the device only to connect to Clover&apos;s local Order Connector. When the cashier taps Round cash, it reads the active Clover order ID, total, and line items. It writes one non-taxable Cash rounding line item when an adjustment is needed, then reads the order again to verify the resulting total.</p>

        <h2>Local preference</h2>
        <p>The selected rounding rule—nearest nickel, always down, or always up—is stored only in the app&apos;s private preferences on that Clover device.</p>

        <h2>No data sent to NickelTap</h2>
        <p>The Android app does not request internet access and does not transmit transaction, customer, employee, payment-card, or merchant data to the app operator. Clover continues to store and process order and payment information under Clover&apos;s own policies.</p>

        <h2>Deletion</h2>
        <p>Clearing NickelTap&apos;s app data or uninstalling it removes its local preference. Cash rounding line items already written to Clover orders remain part of those merchant records.</p>

        <h2>Contact</h2>
        <p>Support and privacy inquiries should use the operator contact published with the app&apos;s Clover App Market listing.</p>
      </article>
    </main>
  );
}
