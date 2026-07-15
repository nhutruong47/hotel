import { Suspense } from 'react';
import { ExperiencesPage } from '../../features/content/ContentPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <ExperiencesPage />
    </Suspense>
  );
}
