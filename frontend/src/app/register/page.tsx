import { Suspense } from 'react';
import { RegisterPage } from '../../features/auth/AuthPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <RegisterPage />
    </Suspense>
  );
}
