import { Suspense } from 'react';
import { AboutPage } from '../../features/content/pages/AboutPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <AboutPage />
    </Suspense>
  );
}
