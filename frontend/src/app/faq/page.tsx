import { Suspense } from 'react';
import { FAQPage } from '../../features/content/ContentPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <FAQPage />
    </Suspense>
  );
}
