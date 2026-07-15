'use client';

import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';
import Link from 'next/link';
import Image from 'next/image';

function StarRating({ rating, size = 'md' }: { rating: number; size?: 'sm' | 'md' }) {
  const sizeClass = size === 'sm' ? 'text-sm' : 'text-lg';
  return (
    <div className={`flex gap-0.5 ${sizeClass}`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} className={star <= rating ? 'text-amber-400' : 'text-gray-300'}>
          ★
        </span>
      ))}
    </div>
  );
}

export function ReviewsPage() {
  const { data: reviews, isLoading } = useQuery({
    queryKey: ['my-reviews'],
    queryFn: () => api.get<any[]>(API_PATHS.reviews.mine),
  });

  if (isLoading) {
    return <div className="animate-pulse space-y-4">Loading reviews...</div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-semibold text-brand-charcoal">My Reviews</h1>
        <p className="mt-2 text-sm text-brand-ink/60">Manage the reviews you've written for past stays.</p>
      </div>

      {!reviews || reviews.length === 0 ? (
        <div className="rounded-[1.5rem] bg-brand-paper p-10 text-center shadow-[0_4px_24px_rgba(32,52,43,0.05)]">
          <p className="text-brand-ink/60 mb-6">You haven't written any reviews yet.</p>
          <Link
            href="/account/bookings"
            className="inline-flex h-12 items-center justify-center rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
          >
            Review a past stay
          </Link>
        </div>
      ) : (
        <div className="grid gap-6">
          {reviews.map((review) => (
            <div key={review.id} className="rounded-[1.5rem] bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.05)]">
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                <div>
                  <h3 className="font-semibold text-brand-charcoal text-lg">{review.room?.name || `Villa ${review.room?.roomNumber}`}</h3>
                  <div className="flex items-center gap-3 mt-1 text-sm text-brand-ink/60">
                    <span>Booking #{review.booking?.id}</span>
                    <span>·</span>
                    <span>{new Date(review.createdAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                  </div>
                  
                  <div className="mt-4">
                    <StarRating rating={review.rating} />
                    <p className="mt-3 text-brand-charcoal leading-relaxed">"{review.comment}"</p>
                  </div>
                  
                  {/* Category Ratings */}
                  <div className="mt-5 grid grid-cols-2 sm:grid-cols-3 gap-y-3 gap-x-6 text-sm">
                    {review.ratingCleanliness != null && (
                      <div className="flex justify-between items-center">
                        <span className="text-brand-ink/70">Cleanliness</span>
                        <span className="font-medium">{review.ratingCleanliness}/5</span>
                      </div>
                    )}
                    {review.ratingService != null && (
                      <div className="flex justify-between items-center">
                        <span className="text-brand-ink/70">Service</span>
                        <span className="font-medium">{review.ratingService}/5</span>
                      </div>
                    )}
                    {review.ratingLocation != null && (
                      <div className="flex justify-between items-center">
                        <span className="text-brand-ink/70">Location</span>
                        <span className="font-medium">{review.ratingLocation}/5</span>
                      </div>
                    )}
                    {review.ratingValue != null && (
                      <div className="flex justify-between items-center">
                        <span className="text-brand-ink/70">Value</span>
                        <span className="font-medium">{review.ratingValue}/5</span>
                      </div>
                    )}
                    {review.ratingAmenities != null && (
                      <div className="flex justify-between items-center">
                        <span className="text-brand-ink/70">Amenities</span>
                        <span className="font-medium">{review.ratingAmenities}/5</span>
                      </div>
                    )}
                  </div>
                </div>
                
                <div className="shrink-0 w-full sm:w-40 aspect-[4/3] rounded-xl overflow-hidden bg-brand-stone relative">
                  {review.room?.imageUrl && (
                    <Image src={review.room.imageUrl} alt="Villa" fill className="object-cover" />
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
