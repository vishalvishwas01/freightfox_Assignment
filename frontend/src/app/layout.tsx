import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = { title: 'FreightFox | Lane Optimizer', description: 'Optimize transporter allocation across freight lanes.' };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
