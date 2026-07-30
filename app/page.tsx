import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: { absolute: "NickelTap — Cash rounding for Clover" },
  description: "A one-tap Clover checkout action that moves cash totals to a five-cent amount.",
};

const examples = [
  { total: "$18.51", nearest: "$18.50", down: "$18.50", up: "$18.55" },
  { total: "$18.52", nearest: "$18.50", down: "$18.50", up: "$18.55" },
  { total: "$18.53", nearest: "$18.55", down: "$18.50", up: "$18.55" },
  { total: "$18.54", nearest: "$18.55", down: "$18.50", up: "$18.55" },
];

export default function Home() {
  return (
    <main className="site-shell">
      <header className="site-header">
        <Link className="brand" href="/" aria-label="NickelTap home">
          <span className="coin" aria-hidden="true">5¢</span>
          <span>NickelTap</span>
        </Link>
        <nav aria-label="Site navigation">
          <Link href="/support">Setup</Link>
          <Link href="/privacy">Privacy</Link>
        </nav>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Clover cash checkout utility</p>
          <h1>Cash totals, rounded to a nickel.</h1>
          <p className="lede">
            NickelTap puts a <strong>Round cash</strong> action on Clover&apos;s payment screen.
            One tap updates the open order total before the cashier selects Cash.
          </p>
          <div className="plain-facts" aria-label="Product facts">
            <span>No donations</span>
            <span>No account</span>
            <span>No cloud service</span>
          </div>
        </div>

        <div className="checkout-card" aria-label="Example Clover checkout">
          <div className="checkout-top"><span>Checkout</span><span>Cash sale</span></div>
          <div className="total-row"><span>Total</span><strong>$18.53</strong></div>
          <button type="button" tabIndex={-1}>Round cash</button>
          <div className="adjustment-row"><span>Cash rounding</span><strong>+$0.02</strong></div>
          <div className="new-total"><span>New total</span><strong>$18.55</strong></div>
        </div>
      </section>

      <section className="section workflow" aria-labelledby="workflow-title">
        <div>
          <p className="eyebrow">How it works</p>
          <h2 id="workflow-title">One setting. One checkout tap.</h2>
        </div>
        <ol className="steps">
          <li><span>1</span><div><strong>Choose the rule</strong><p>Open NickelTap once and select nearest, always down, or always up.</p></div></li>
          <li><span>2</span><div><strong>Tap Round cash</strong><p>At Clover checkout, use the action only when the customer will pay cash.</p></div></li>
          <li><span>3</span><div><strong>Take the cash</strong><p>NickelTap verifies the updated total and returns directly to Clover checkout.</p></div></li>
        </ol>
      </section>

      <section className="section rules" aria-labelledby="rules-title">
        <div className="section-heading">
          <p className="eyebrow">Three local rules</p>
          <h2 id="rules-title">The merchant decides which way cents move.</h2>
          <p>The preference stays on the Clover device. A total already ending in 0 or 5 is left unchanged.</p>
        </div>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Original</th><th>Nearest</th><th>Down</th><th>Up</th></tr></thead>
            <tbody>{examples.map((row) => (
              <tr key={row.total}><th>{row.total}</th><td>{row.nearest}</td><td>{row.down}</td><td>{row.up}</td></tr>
            ))}</tbody>
          </table>
        </div>
      </section>

      <aside className="cash-only">
        <div className="cash-icon" aria-hidden="true">$</div>
        <div>
          <strong>Cash only, by design.</strong>
          <p>Clover exposes third-party order changes as a cashier action on the payment screen. The cashier must tap Round cash only for a cash sale; card totals should not be rounded.</p>
        </div>
      </aside>

      <footer>
        <span>NickelTap for Clover</span>
        <div><Link href="/support">Support</Link><Link href="/privacy">Privacy</Link><Link href="/terms">Terms</Link></div>
      </footer>
    </main>
  );
}
