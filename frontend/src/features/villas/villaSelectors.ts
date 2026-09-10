import { fallbackVillas } from './villaFallbacks';
import type { Villa } from './types';

export function findVillaById(villas: Villa[] | undefined, villaId: string | undefined) {
  if (!villaId) return null;
  const list = villas ?? fallbackVillas;
  const numId = Number(villaId);
  if (Number.isFinite(numId)) {
    const found = list.find((v) => v.id === numId);
    if (found) return found;
  }
  const clean = decodeURIComponent(villaId).toLowerCase().trim().replace(/[-_]/g, '');
  return (
    list.find((v) => v.slug?.toLowerCase().replace(/[-_]/g, '') === clean) ||
    list.find((v) => v.roomNumber?.toLowerCase() === villaId.toLowerCase().trim()) ||
    list.find((v) => {
      const vSlug = v.slug?.toLowerCase().replace(/[-_]/g, '') || '';
      return vSlug && (clean.startsWith(vSlug) || vSlug.startsWith(clean));
    }) ||
    null
  );
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
