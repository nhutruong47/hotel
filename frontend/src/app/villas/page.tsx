import { Suspense } from 'react';
import { VillasPage } from '../../features/villas/VillasPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <VillasPage />
    </Suspense>
  );
}
