import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Terms of use" };

export default function TermsPage() {
  return (
    <main className="legal-page">
      <nav className="legal-nav" aria-label="Legal navigation">
        <Link className="brand" href="/"><span className="coin">5¢</span><span>NickelTap</span></Link>
        <Link href="/">Back to NickelTap</Link>
      </nav>
      <article>
        <p className="eyebrow">Last updated July 21, 2026</p>
        <h1>Terms of use</h1>
        <p className="legal-intro">NickelTap modifies open Clover orders for cash rounding. It does not collect a donation, process a payment, or choose the tender.</p>

        <h2>Cashier action required</h2>
        <p>The cashier must tap Round cash before selecting Clover&apos;s Cash tender. NickelTap must not be used for card, gift-card, digital-wallet, or other non-cash payments.</p>

        <h2>Merchant responsibilities</h2>
        <p>The merchant is responsible for choosing a lawful rounding rule, training staff, disclosing rounding to customers where required, reviewing receipts and reports, and handling refunds, split tenders, taxes, and accounting according to applicable rules.</p>

        <h2>Order adjustment</h2>
        <p>NickelTap adds one non-taxable, non-revenue line item named Cash rounding. Nearest-nickel mode changes a total by at most two cents. Always-up and always-down modes can change it by at most four cents. A total already divisible by five cents is unchanged.</p>

        <h2>Availability</h2>
        <p>The app depends on Clover&apos;s Android platform and Order Connector. Merchants must validate it on their exact Clover device, software version, service plan, regional configuration, and checkout workflows before production use.</p>

        <h2>Third-party platform</h2>
        <p>Clover is a third-party platform with separate terms. NickelTap is not legal, tax, or accounting advice.</p>
      </article>
    </main>
  );
}
