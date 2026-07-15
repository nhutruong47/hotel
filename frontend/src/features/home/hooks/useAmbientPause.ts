import { useEffect } from 'react';

/**
 * Adds an `.is-paused` class to the element when it is fully outside the
 * viewport. Useful for pausing infinite CSS animations (saves battery
 * without restarting them when the element scrolls back in, since
 * `animation-play-state: paused` simply halts and resumes gracefully).
 */
export function useAmbientPause(element: HTMLElement | null | undefined) {
  useEffect(() => {
    if (!element) return;

    if (typeof IntersectionObserver === 'undefined') {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            element.classList.remove('is-paused');
          } else {
            element.classList.add('is-paused');
          }
        }
      },
      { threshold: 0 },
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, [element]);
}
