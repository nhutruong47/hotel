import { Suspense } from 'react';
import { DiningPage } from '../../features/content/ContentPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <DiningPage />
    </Suspense>
  );
}
