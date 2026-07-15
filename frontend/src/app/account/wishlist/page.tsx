import { Suspense } from 'react';
import { WishlistPage } from '../../../features/account/WishlistPage';

export const metadata = { title: 'Wishlist — Nhu Villas' };

export default function Page() {
  return <Suspense fallback={null}><WishlistPage /></Suspense>;
}
