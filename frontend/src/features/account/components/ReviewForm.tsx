'use client';

import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS } from '../../../shared/api/client';

type ReviewFormProps = {
  bookingId: number;
  roomName: string;
  onClose: () => void;
};

function StarRatingInput({ label, value, onChange }: { label: string; value: number; onChange: (val: number) => void }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm font-semibold text-brand-charcoal">{label}</span>
      <div className="flex gap-1 text-2xl cursor-pointer">
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            onClick={() => onChange(star)}
            className={star <= value ? 'text-amber-400' : 'text-gray-300'}
          >
            ★
          </span>
        ))}
      </div>
    </div>
  );
}

export function ReviewForm({ bookingId, roomName, onClose }: ReviewFormProps) {
  const queryClient = useQueryClient();
  
  // States
  const [rating, setRating] = useState(5);
  const [ratingCleanliness, setRatingCleanliness] = useState(5);
  const [ratingService, setRatingService] = useState(5);
  const [ratingLocation, setRatingLocation] = useState(5);
  const [ratingValue, setRatingValue] = useState(5);
  const [ratingAmenities, setRatingAmenities] = useState(5);
  const [comment, setComment] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const submit = useMutation<{ message: string }, Error, any>({
    mutationFn: (body) => api.post<{ message: string }>(API_PATHS.reviews.submitForBooking(body.bookingId), body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-reviews'] });
      queryClient.invalidateQueries({ queryKey: ['booking', bookingId.toString()] });
      onClose();
    },
    onError: (err: any) => setErrorMsg(err.message || 'Unable to submit review'),
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className="bg-brand-paper w-full max-w-xl rounded-[2rem] p-8 max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-semibold text-brand-charcoal">Rate your stay at {roomName}</h2>
          <button onClick={onClose} className="text-brand-ink/50 hover:text-brand-charcoal p-2">✕</button>
        </div>
        
        {errorMsg && <p className="mb-4 rounded-xl bg-brand-coral/10 p-4 text-sm text-brand-charcoal">{errorMsg}</p>}

        <form
          onSubmit={(e) => {
            e.preventDefault();
            submit.mutate({
              bookingId,
              rating,
              ratingCleanliness,
              ratingService,
              ratingLocation,
              ratingValue,
              ratingAmenities,
              comment
            });
          }}
          className="space-y-6"
        >
          {/* Main Rating */}
          <div className="bg-brand-white rounded-[1.5rem] p-5 shadow-sm">
             <div className="flex flex-col items-center">
                <span className="text-sm font-semibold uppercase tracking-widest text-brand-sage mb-2">Overall Experience</span>
                <div className="flex gap-2 text-4xl cursor-pointer">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <span
                      key={star}
                      onClick={() => setRating(star)}
                      className={star <= rating ? 'text-amber-400' : 'text-gray-200 hover:text-amber-200'}
                    >
                      ★
                    </span>
                  ))}
                </div>
             </div>
          </div>

          <div className="bg-brand-white rounded-[1.5rem] p-5 shadow-sm space-y-4">
             <h3 className="text-xs font-semibold uppercase tracking-widest text-brand-sage mb-2 border-b border-brand-stone pb-2">Category Ratings</h3>
             <StarRatingInput label="Cleanliness" value={ratingCleanliness} onChange={setRatingCleanliness} />
             <StarRatingInput label="Service & Staff" value={ratingService} onChange={setRatingService} />
             <StarRatingInput label="Location" value={ratingLocation} onChange={setRatingLocation} />
             <StarRatingInput label="Value for Money" value={ratingValue} onChange={setRatingValue} />
             <StarRatingInput label="Amenities & Facilities" value={ratingAmenities} onChange={setRatingAmenities} />
          </div>

          <div>
             <label className="text-sm font-semibold text-brand-charcoal block mb-2">Your feedback (optional)</label>
             <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Share your experience to help other travelers..."
                className="w-full rounded-[1.25rem] border border-brand-stone bg-brand-white p-5 min-h-32 resize-none"
             />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-brand-stone">
             <button
               type="button"
               onClick={onClose}
               className="rounded-full px-6 py-3 font-semibold text-sm uppercase tracking-widest text-brand-ink hover:bg-brand-stone"
             >
               Cancel
             </button>
             <button
               type="submit"
               disabled={submit.isPending}
               className="rounded-full bg-brand-forest px-8 py-3 text-sm font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep disabled:opacity-50"
             >
               {submit.isPending ? 'Submitting...' : 'Submit Review'}
             </button>
          </div>
        </form>
      </div>
    </div>
  );
}
