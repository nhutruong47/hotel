import { Suspense } from 'react';
import { SupportPage } from '../../../features/account/SupportPage';

export const metadata = { title: 'Support — Nhu Villas' };

export default function Page() {
  return <Suspense fallback={null}><SupportPage /></Suspense>;
}
