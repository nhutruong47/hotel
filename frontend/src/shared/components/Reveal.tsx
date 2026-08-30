'use client';

import { useEffect, useRef, type ElementType, type ReactNode } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

type RevealProps = {
  children: ReactNode;
  as?: ElementType;
  className?: string;
  /** Translate distance in px (default 48). */
  y?: number;
  /** Translate X distance in px (default 0). */
  x?: number;
  /** Starting scale (default 1). */
  scale?: number;
  /** Starting rotation (default 0). */
  rotation?: number;
  /** Animation duration in seconds (default 1.1). */
  duration?: number;
  /** Delay in seconds before the tween starts (default 0). */
  delay?: number;
  /** scrollTrigger.start (default 'top 85%'). */
  start?: string;
  /** Stagger children by this many seconds. */
  stagger?: number;
  /** Selector relative to root for stagger grouping (default direct children). */
  staggerSelector?: string;
};

/**
 * Wrap any block of JSX to fade & slide it in on scroll.
 * Honors prefers-reduced-motion by skipping the tween.
 */
export function Reveal({
  children,
  as: Element = 'div',
  className = '',
  y = 48,
  x = 0,
  scale = 1,
  rotation = 0,
  duration = 1.1,
  delay = 0,
  start = 'top 85%',
  stagger,
  staggerSelector,
}: RevealProps) {
  const ref = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const reduce =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduce) {
      gsap.set(el, { opacity: 1, y: 0 });
      return;
    }

    gsap.set(el, { opacity: 0, y });

    const targets =
      stagger && staggerSelector
        ? el.querySelectorAll(staggerSelector)
        : stagger
          ? Array.from(el.children)
          : [el];

    gsap.set(targets, { opacity: 0, y, x, scale, rotation });

    const tween = gsap.to(targets, {
      opacity: 1,
      y: 0,
      x: 0,
      scale: 1,
      rotation: 0,
      duration,
      delay,
      stagger: stagger ?? 0,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: el,
        start,
        toggleActions: 'play none none none',
      },
    });

    return () => {
      tween.scrollTrigger?.kill();
      tween.kill();
    };
  }, [delay, duration, start, stagger, staggerSelector, y, x, scale, rotation]);

  return (
    // @ts-ignore
    <Element ref={ref as any} className={className}>
      {children}
    </Element>
  );
}

export default Reveal;
