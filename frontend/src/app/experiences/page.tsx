import { Suspense } from 'react';
import { ExperiencesPage } from '../../features/content/pages/ExperiencesPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <ExperiencesPage />
    </Suspense>
  );
}
