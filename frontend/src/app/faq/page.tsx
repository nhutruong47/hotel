import { Suspense } from 'react';
import { FAQPage } from '../../features/content/pages/FAQPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <FAQPage />
    </Suspense>
  );
}
