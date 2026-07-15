import { Suspense } from 'react';
import { NotificationsPage } from '../../../features/account/NotificationsPage';

export const metadata = { title: 'Notifications — Nhu Villas' };

export default function Page() {
  return <Suspense fallback={null}><NotificationsPage /></Suspense>;
}
