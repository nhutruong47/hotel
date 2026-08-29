import { Suspense } from 'react';
import { GalleryPage } from '../../features/content/pages/GalleryPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <GalleryPage />
    </Suspense>
  );
}
