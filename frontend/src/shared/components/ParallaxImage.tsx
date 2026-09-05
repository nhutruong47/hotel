'use client';

import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

export function ParallaxImage({ 
  src, 
  alt, 
  className = '' 
}: { 
  src: string; 
  alt: string; 
  className?: string; 
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion || !containerRef.current || !imageRef.current) return;

    // Create a timeline that moves the image slightly slower than the scroll speed
    // by animating its y position. The image needs to be taller than the container (scale > 1)
    
    // Set initial scale to ensure there's room to scrub without showing background
    gsap.set(imageRef.current, { scale: 1.15, transformOrigin: 'center center' });

    const tl = gsap.timeline({
      scrollTrigger: {
        trigger: containerRef.current,
        start: 'top bottom', // when top of container hits bottom of viewport
        end: 'bottom top',   // when bottom of container hits top of viewport
        scrub: true,
      },
    });

    tl.fromTo(
      imageRef.current,
      { yPercent: -10 }, // start slightly up
      { yPercent: 10, ease: 'none' } // move down as we scroll down
    );

    return () => {
      tl.kill();
    };
  }, []);

  return (
    <div ref={containerRef} className={`overflow-hidden relative h-full w-full ${className}`}>
      <img
        ref={imageRef}
        src={src}
        alt={alt}
        className="absolute inset-0 h-full w-full object-cover transition duration-700 ease-[cubic-bezier(0.16,1,0.3,1)] hover:scale-[1.2]"
        loading="lazy"
        decoding="async"
      />
    </div>
  );
}
