import { useEffect, useState } from 'react';

/**
 * Tracks vertical scroll position. Throttled with rAF so consumers
 * reading the value in JSX don't trigger an extra render per pixel.
 *
 * Returns the current Y. Components that only care about thresholds
 * (e.g. navbar scroll-shrink) should pass their own check and subscribe
 * to the boolean result rather than the raw value.
 */
export function useScrollY(threshold = 0): number {
  const [y, setY] = useState(0);

  useEffect(() => {
    let raf = 0;
    const onScroll = () => {
      if (raf) return;
      raf = requestAnimationFrame(() => {
        raf = 0;
        const next = window.scrollY || window.pageYOffset || 0;
        setY((current) => (Math.abs(current - next) >= threshold ? next : current));
      });
    };

    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('scroll', onScroll);
    };
  }, [threshold]);

  return y;
}
