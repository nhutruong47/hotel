import { Suspense } from 'react';
import { SettingsPage } from '../../../features/account/SettingsPage';

export const metadata = { title: 'Settings — Nhu Villas' };

export default function Page() {
  return <Suspense fallback={null}><SettingsPage /></Suspense>;
}
