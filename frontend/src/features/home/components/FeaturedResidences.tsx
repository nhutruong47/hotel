'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Reveal } from '../../../shared/components/Reveal';
import { useTranslation, useCurrency } from '../../../shared/i18n/hooks';
import { ParallaxImage } from '../../../shared/components/ParallaxImage';

// Dummy data to match the design. In a real app, this would come from the API.
const residences = [
  {
    id: 'residence-1',
    eyebrow: 'RESIDENCE',
    title: 'Hillside Residence',
    desc: 'A larger private residence designed for hosted dinners, longer retreats, and unforgettable sunset pool rituals on the ridge.',
    capacity: '6 Guests',
    price: 1150,
    reviews: 19,
    rating: 5,
    image: '/images/nhu-infinity-pool.jpg', // Re-using existing image
  },
  {
    id: 'residence-2',
    eyebrow: 'POOL VILLA',
    title: 'Water Courtyard Villa',
    desc: 'A tranquil courtyard villa arranged around reflective water features, featuring fine linens, warm stone elements, and spaces for late breakfast rituals.',
    reviews: 61,
    rating: 5,
    image: '/images/nhu-garden-pool-villa.jpg', // Re-using existing image
  },
  {
    id: 'residence-3',
    eyebrow: 'PAVILION',
    title: 'Forest Pavilion',
    desc: 'A quiet pavilion designed for couples seeking deep shade, the natural sounds of the forest, and an intimate, smaller footprint.',
    reviews: 32,
    rating: 5,
    image: '/images/nhu-villa-interior.jpg', // Re-using existing image
  }
];

function StarRating({ rating, size = 'md' }: { rating: number; size?: 'sm' | 'md' }) {
  const sizeClass = size === 'sm' ? 'text-[10px]' : 'text-sm';
  return (
    <div className={`flex gap-1 ${sizeClass}`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} className={star <= rating ? 'text-amber-400' : 'text-gray-300'}>
          ★
        </span>
      ))}
    </div>
  );
}

export function FeaturedResidences() {
  const { formatCurrency } = useCurrency();
  
  const hillsideImages = [
    residences[0].image,
    '/images/nhu-villa-interior.jpg',
    '/images/nhu-private-dining.jpg'
  ];
  
  const [currentSlide, setCurrentSlide] = useState(0);
  
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % hillsideImages.length);
    }, 4000);
    return () => clearInterval(timer);
  }, [hillsideImages.length]);
  
  return (
    <section className="bg-brand-paper py-24 lg:py-36">
      
      {/* ROW 1: Image Left, Text Right (FULL WIDTH) */}
      <div className="mb-24 relative w-full py-24 lg:py-48">
        
        {/* Background glow and sketch art */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none [mask-image:radial-gradient(ellipse_200%_80%_at_50%_50%,black_40%,transparent_100%)] [-webkit-mask-image:radial-gradient(ellipse_200%_80%_at_50%_50%,black_40%,transparent_100%)]">
          <div className="absolute inset-0 bg-brand-stone" />
          <img 
            src="/images/nature-line-art.jpg" 
            alt="Nature line art background"
            className="absolute inset-0 h-full w-full object-cover opacity-15 invert mix-blend-multiply"
          />
        </div>

        <div className="relative mx-auto max-w-[1440px] px-5 sm:px-8 lg:px-12 xl:px-16 z-10">
          <div className="grid gap-12 lg:grid-cols-[0.62fr_0.38fr] lg:items-center lg:gap-20 xl:gap-32">
            
            <div className="relative aspect-square w-full lg:aspect-[1.1/1]">
              {/* Main Big Stone */}
              <Reveal delay={0.2} duration={1.5} x={-60} scale={0.95} className="absolute left-0 top-[10%] h-[75%] w-[70%] overflow-hidden rounded-[48%_52%_68%_32%/53%_43%_57%_47%] shadow-[0_20px_50px_rgba(0,0,0,0.5)] z-10 hover:scale-[1.02] transition-transform duration-700">
                <img 
                  src={hillsideImages[0]} 
                  alt="Hillside Pool"
                  className="h-full w-full object-cover"
                />
              </Reveal>
              
              {/* Top Right Stone */}
              <Reveal delay={0.5} duration={1.5} x={40} y={-40} scale={0.9} className="absolute right-[5%] top-0 h-[45%] w-[45%] overflow-hidden rounded-[63%_37%_42%_58%/46%_59%_41%_54%] shadow-[0_20px_40px_rgba(0,0,0,0.4)] z-20 hover:scale-[1.03] transition-transform duration-700">
                <img 
                  src={hillsideImages[1]} 
                  alt="Villa Interior"
                  className="h-full w-full object-cover"
                />
              </Reveal>
              
              {/* Bottom Right Stone */}
              <Reveal delay={0.8} duration={1.5} x={40} y={40} scale={0.9} className="absolute bottom-[5%] right-0 h-[50%] w-[55%] overflow-hidden rounded-[38%_62%_51%_49%/58%_42%_58%_42%] shadow-[0_20px_40px_rgba(0,0,0,0.4)] z-30 hover:scale-[1.03] transition-transform duration-700">
                <img 
                  src={hillsideImages[2]} 
                  alt="Private Dining"
                  className="h-full w-full object-cover"
                />
              </Reveal>
            </div>  

            <Reveal className="flex flex-col justify-center lg:pr-4 xl:pr-8">
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-brand-ink/50">
                {residences[0].eyebrow}
              </p>
              <h2 className="mt-5 font-serif text-4xl leading-[1.1] text-brand-ink sm:text-5xl lg:text-[4.25rem]">
                {residences[0].title.split(' ').map((word, i) => (
                  <React.Fragment key={i}>
                    {word}
                    {i === 0 && <br />}
                  </React.Fragment>
                ))}
              </h2>
              <p className="mt-8 max-w-[340px] text-sm leading-[1.8] text-brand-ink/70">
                {residences[0].desc}
              </p>
              
              <div className="mt-14 flex max-w-[340px] items-end justify-between border-t border-brand-ink/20 pt-6">
                <div>
                  <p className="text-[9px] font-semibold uppercase tracking-[0.2em] text-brand-ink/50">
                    Capacity
                  </p>
                  <p className="mt-2 text-sm font-medium text-brand-ink">
                    {residences[0].capacity}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-[9px] font-semibold uppercase tracking-[0.2em] text-brand-ink/50">
                    Starting at
                  </p>
                  <p className="mt-2 text-sm font-medium text-brand-ink">
                    {formatCurrency(residences[0].price)}
                  </p>
                </div>
              </div>
              
              <div className="mt-10 flex items-center gap-3">
                <StarRating rating={residences[0].rating} size="sm" />
                <span className="text-xs text-brand-ink/50">({residences[0].reviews})</span>
              </div>
            </Reveal>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-[1440px] px-5 sm:px-8 lg:px-12 xl:px-16">

        {/* ROW 2: Two Columns */}
        <div className="mx-auto mt-12 grid max-w-[1280px] items-start gap-12 lg:mt-32 lg:grid-cols-[0.85fr_1.15fr] lg:gap-24 xl:gap-32">
          
          {/* Left Column: Image Top, Text Bottom */}
          <Reveal delay={0.1} y={40} className="flex flex-col">
            <div className="relative aspect-[4/5] w-full overflow-hidden rounded-b-[1.5rem] rounded-t-[10rem]">
              <img 
                src={residences[1].image} 
                alt={residences[1].title}
                className="h-full w-full object-cover transition-transform duration-1000 hover:scale-[1.03]"
              />
            </div>
            <div className="mt-8 px-2">
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-brand-ink/50">
                {residences[1].eyebrow}
              </p>
              <h3 className="mt-3 font-serif text-2xl text-brand-charcoal">
                {residences[1].title}
              </h3>
              <p className="mt-4 text-sm leading-relaxed text-brand-ink/70">
                {residences[1].desc}
              </p>
              <div className="mt-6 flex items-center gap-3">
                <StarRating rating={residences[1].rating} size="sm" />
                <span className="text-xs text-brand-ink/60">({residences[1].reviews})</span>
              </div>
            </div>
          </Reveal>

          {/* Right Column: Large Image with Overlapping Card */}
          <Reveal delay={0.3} y={60} className="relative mt-12 lg:mt-0">
            <div className="relative aspect-[4/5] w-full overflow-hidden rounded-bl-[6rem] rounded-tr-[6rem] rounded-br-[1.5rem] rounded-tl-[1.5rem]">
              <img 
                src={residences[2].image} 
                alt={residences[2].title}
                className="h-full w-full object-cover transition-transform duration-1000 hover:scale-[1.03]"
              />
            </div>
            
            {/* Floating Overlap Card */}
            <div className="relative -mt-16 ml-auto mr-4 w-[90%] max-w-[360px] rounded-[1.25rem] bg-brand-sand p-8 shadow-[0_15px_40px_rgba(32,52,43,0.06)] lg:absolute lg:-bottom-12 lg:-left-16 lg:m-0">
              <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-brand-ink/50">
                {residences[2].eyebrow}
              </p>
              <h3 className="mt-3 font-serif text-2xl text-brand-charcoal">
                {residences[2].title}
              </h3>
              <p className="mt-4 text-sm leading-relaxed text-brand-ink/70">
                {residences[2].desc}
              </p>
              <div className="mt-6 flex items-center gap-3">
                <StarRating rating={residences[2].rating} size="sm" />
                <span className="text-xs text-brand-ink/60">({residences[2].reviews})</span>
              </div>
            </div>
          </Reveal>

        </div>
      </div>
    </section>
  );
}
