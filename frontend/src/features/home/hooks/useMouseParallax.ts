import { useEffect } from 'react';

/**
 * Applies a translate3d transform to each ref provided.
 * `speeds` controls how much each layer moves (0 = still, 1 = follows pointer 1:1).
 * The actual pixel displacement is capped at `maxPx` to keep motion extremely subtle.
 *
 * Elements should be positioned with will-change: transform and no other
 * transform animations on the same axis, so this composes cleanly with CSS.
 */
export function useMouseParallax<T extends HTMLElement = HTMLElement>(
  refs: Array<{ current: T | null }>,
  speeds: number[],
  maxPx = 8,
) {
  useEffect(() => {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduce || refs.length === 0) return;

    let raf = 0;
    let x = 0;
    let y = 0;

    const apply = () => {
      refs.forEach((ref, index) => {
        const el = ref.current;
        if (!el) return;
        const speed = speeds[index] ?? 0;
        if (speed === 0) return;
        const tx = x * speed * maxPx;
        const ty = y * speed * maxPx;
        el.style.transform = `translate3d(${tx.toFixed(2)}px, ${ty.toFixed(2)}px, 0)`;
      });
      raf = 0;
    };

    const onMove = (event: PointerEvent) => {
      const w = window.innerWidth;
      const h = window.innerHeight;
      x = (event.clientX / w) * 2 - 1;
      y = (event.clientY / h) * 2 - 1;
      if (!raf) raf = requestAnimationFrame(apply);
    };

    const onLeave = () => {
      x = 0;
      y = 0;
      if (!raf) raf = requestAnimationFrame(apply);
    };

    window.addEventListener('pointermove', onMove, { passive: true });
    document.addEventListener('mouseleave', onLeave);

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('pointermove', onMove);
      document.removeEventListener('mouseleave', onLeave);
      refs.forEach((ref) => {
        if (ref.current) ref.current.style.transform = '';
      });
    };
  }, [refs, speeds, maxPx]);
}
