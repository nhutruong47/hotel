import { Suspense } from 'react';
import type { Metadata } from 'next';
import { VillaDetailPage } from '../../../features/villas/VillaDetailPage';
import { fallbackVillas } from '../../../features/villas/villaFallbacks';
import { findVillaById } from '../../../features/villas/villaSelectors';

interface PageProps {
  params: Promise<{ villaId: string }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { villaId } = await params;
  const villa = findVillaById(fallbackVillas, villaId);

  if (!villa) {
    return {
      title: 'Villa Not Found | Nhu Villas',
      description: 'The requested luxury villa retreat could not be found.',
    };
  }

  const title = `${villa.name} (${villa.roomNumber}) | Nhu Villas`;
  const description = villa.description || `Experience peaceful luxury at ${villa.name}. Private pool, tranquil surroundings, and refined hospitality.`;
  const url = `https://nhuvillas.com/villas/${villa.slug || villa.id}`;
  const ogImage = villa.imageUrl.startsWith('http') ? villa.imageUrl : `https://nhuvillas.com${villa.imageUrl}`;

  return {
    title,
    description,
    alternates: {
      canonical: url,
    },
    openGraph: {
      title,
      description,
      url,
      type: 'website',
      images: [
        {
          url: ogImage,
          width: 1600,
          height: 900,
          alt: villa.name,
        },
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title,
      description,
      images: [ogImage],
    },
  };
}

export default async function Page({ params }: PageProps) {
  const { villaId } = await params;
  const villa = findVillaById(fallbackVillas, villaId);

  const jsonLd = villa
    ? {
        '@context': 'https://schema.org',
        '@type': 'LodgingBusiness',
        name: `${villa.name} - Nhu Villas`,
        description: villa.description,
        image: villa.imageUrl.startsWith('http') ? villa.imageUrl : `https://nhuvillas.com${villa.imageUrl}`,
        url: `https://nhuvillas.com/villas/${villa.slug || villa.id}`,
        priceRange: `$${villa.pricePerNight}/night`,
        numberOfRooms: villa.bedrooms,
        maximumAttendeeCapacity: villa.capacity,
        aggregateRating: {
          '@type': 'AggregateRating',
          ratingValue: villa.rating || 4.9,
          reviewCount: villa.reviewCount || 10,
        },
        address: {
          '@type': 'PostalAddress',
          streetAddress: 'Tuyen Lam Lake Eco-Resort',
          addressLocality: 'Da Lat',
          addressRegion: 'Lam Dong',
          addressCountry: 'VN',
        },
      }
    : null;

  return (
    <>
      {jsonLd && (
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      )}
      <Suspense fallback={null}>
        <VillaDetailPage />
      </Suspense>
    </>
  );
}
