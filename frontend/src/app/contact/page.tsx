import { Suspense } from 'react';
import { ContactPage } from '../../features/content/pages/ContactPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <ContactPage />
    </Suspense>
  );
}
