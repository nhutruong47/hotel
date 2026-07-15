import { Suspense } from 'react';
import { VillaDetailPage } from '../../../features/villas/VillaDetailPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <VillaDetailPage />
    </Suspense>
  );
}
