import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "NickelTap", template: "%s · NickelTap" },
  description: "A one-tap cash-rounding action for Clover checkout.",
  applicationName: "NickelTap",
  robots: { index: true, follow: true },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
