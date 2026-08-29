import { Suspense } from 'react';
import { DiningPage } from '../../features/content/pages/DiningPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <DiningPage />
    </Suspense>
  );
}
