import { Suspense } from 'react';
import { OffersPage } from '../../features/content/pages/OffersPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <OffersPage />
    </Suspense>
  );
}
