import { fallbackVillas } from './villaFallbacks';
import type { Villa } from './types';

export function findVillaById(villas: Villa[] | undefined, villaId: string | undefined) {
  const id = Number(villaId);

  if (!Number.isFinite(id)) {
    return null;
  }

  return (villas ?? fallbackVillas).find((villa) => villa.id === id) ?? null;
}

export function getSimilarVillas(currentVilla: Villa, villas: Villa[] | undefined) {
  return (villas ?? fallbackVillas)
    .filter((villa) => villa.id !== currentVilla.id)
    .sort((a, b) => {
      const typeScore = Number(b.roomType === currentVilla.roomType) - Number(a.roomType === currentVilla.roomType);
      const priceScore =
        Math.abs(a.pricePerNight - currentVilla.pricePerNight) -
        Math.abs(b.pricePerNight - currentVilla.pricePerNight);

      return typeScore || priceScore;
    })
    .slice(0, 3);
}
