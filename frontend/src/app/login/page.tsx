import { Suspense } from 'react';
import { LoginPage } from '../../features/auth/AuthPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <LoginPage />
    </Suspense>
  );
}
