import { Suspense } from 'react';
import { BookingsPage } from '../../../features/account/BookingsPage';

export const metadata = { title: 'My Bookings — Nhu Villas' };

export default function Page() {
  return <Suspense fallback={null}><BookingsPage /></Suspense>;
}
