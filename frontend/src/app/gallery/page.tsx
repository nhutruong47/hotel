import { Suspense } from 'react';
import { GalleryPage } from '../../features/content/ContentPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <GalleryPage />
    </Suspense>
  );
}
