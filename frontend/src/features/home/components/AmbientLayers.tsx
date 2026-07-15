import { useEffect, useRef } from 'react';
import { useAmbientPause } from '../hooks/useAmbientPause';

const DUST_COUNT = 14;
const GLINT_COUNT = 6;

/**
 * Atmospheric non-canvas layers overlaid on top of the hero image.
 * Pure CSS keyframes (driven from index.css). Kept decorative and
 * extremely low-contrast so it never distracts from the booking CTA.
 *
 * Layers, back-to-front:
 *   1. Sunlight rays (very low opacity, very slow drift)
 *   2. Tree-shadow breeze (subtle duotone wash)
 *   3. Mist bands (3 layered radials)
 *   4. Sun glints (fixed-position drifting ellipses)
 *   5. Dust motes (small particles floating upward)
 */
export function AmbientLayers() {
  const rootRef = useRef<HTMLDivElement | null>(null);
  useAmbientPause(rootRef.current);

  // Pre-compute deterministic positions for dust & glints so SSR/CSR match.
  const dust = Array.from({ length: DUST_COUNT }, (_, i) => ({
    id: `dust-${i}`,
    left: (i * 7.13) % 100,
    bottom: (i * 5.7) % 80,
    size: 2 + ((i * 3) % 4),
    delay: (i * 1.4) % 12,
    duration: 14 + ((i * 2.3) % 10),
    opacity: 0.18 + ((i * 0.05) % 0.18),
  }));

  const glints = Array.from({ length: GLINT_COUNT }, (_, i) => ({
    id: `glint-${i}`,
    top: 18 + ((i * 11.7) % 70),
    left: 12 + ((i * 13.3) % 76),
    size: 36 + ((i * 7) % 48),
    delay: (i * 2.1) % 9,
    duration: 8 + ((i * 1.7) % 5),
  }));

  // Re-trigger dust animations on mount so they don't all start at the same time.
  useEffect(() => {
    if (!rootRef.current) return;
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduce) return;
    const elements = rootRef.current.querySelectorAll<HTMLElement>('[data-dust]');
    elements.forEach((el) => {
      const delay = Number(el.dataset.dust ?? 0);
      el.style.animationDelay = `-${delay}s`;
    });
  }, []);

  return (
    <div
      ref={rootRef}
      aria-hidden="true"
      className="ambient-layers pointer-events-none absolute inset-0 isolate overflow-hidden mix-blend-screen"
    >
      <div className="ambient-sunrays absolute inset-0" />
      <div className="ambient-breeze absolute inset-0 mix-blend-multiply" />

      <div className="ambient-mist absolute inset-x-0 bottom-0 h-[55%]">
        <div className="ambient-mist-a absolute inset-0" />
        <div className="ambient-mist-b absolute inset-0" />
        <div className="ambient-mist-c absolute inset-0" />
      </div>

      <div className="ambient-glints absolute inset-0">
        {glints.map((g) => (
          <span
            key={g.id}
            className="ambient-glint"
            style={{
              top: `${g.top}%`,
              left: `${g.left}%`,
              width: `${g.size}px`,
              height: `${g.size}px`,
              animationDelay: `-${g.delay}s`,
              animationDuration: `${g.duration}s`,
            }}
          />
        ))}
      </div>

      <div className="ambient-dust absolute inset-0">
        {dust.map((d) => (
          <span
            key={d.id}
            data-dust={d.delay}
            className="ambient-dust-particle"
            style={{
              left: `${d.left}%`,
              bottom: `${d.bottom}%`,
              width: `${d.size}px`,
              height: `${d.size}px`,
              opacity: d.opacity,
              animationDelay: `-${d.delay}s`,
              animationDuration: `${d.duration}s`,
            }}
          />
        ))}
      </div>
    </div>
  );
}

export default AmbientLayers;
