import { Suspense } from 'react';
import { ContactPage } from '../../features/content/ContentPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <ContactPage />
    </Suspense>
  );
}
