import { api, API_PATHS } from '../../shared/api/client';
import { fallbackVillas } from './villaFallbacks';
import type { Villa, VillaAmenity, VillaPromotion } from './types';

type RawRoomType = {
  id?: number;
  name?: string;
  description?: string;
};

type RawRoom = {
  id?: number;
  roomNumber?: string;
  roomType?: RawRoomType | string;
  roomTypeDisplayName?: string;
  amenities?: unknown;
  pricePerNight?: number | string;
  description?: string;
  imageUrl?: string;
  isAvailable?: boolean;
  capacity?: number;
  bedrooms?: number;
  avgRating?: number;
  reviewCount?: number;
  promotions?: unknown[];
};

type RoomListResponse = {
  rooms: RawRoom[];
  roomTypes: RawRoomType[];
};

type RoomDetailResponse = {
  room: RawRoom;
  avgRating?: number;
  reviewCount?: number;
  reviews?: unknown[];
};

const fallbackImages = fallbackVillas.map((villa) => villa.imageUrl);

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function toNumber(value: unknown, fallback: number): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string') {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return fallback;
}

function normalizeAmenities(value: unknown): VillaAmenity[] {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => {
      if (typeof item === 'string') return { name: item } as VillaAmenity;
      if (isRecord(item) && typeof item.name === 'string') {
        return {
          id: typeof item.id === 'number' ? item.id : undefined,
          name: item.name,
        } as VillaAmenity;
      }
      return null;
    })
    .filter((item): item is VillaAmenity => item !== null);
}

function getRoomTypeName(roomType: RawRoom['roomType'], displayName: unknown): string {
  if (typeof displayName === 'string' && displayName.length > 0) return displayName;
  if (typeof roomType === 'string') return roomType;
  if (roomType && typeof roomType.name === 'string') return roomType.name;
  return 'Private Villa';
}

function getImageUrl(value: unknown, fallback: string): string {
  if (typeof value !== 'string' || value.length === 0) return fallback;
  if (value.startsWith('http')) return fallback;
  return value;
}

function normalizeRoom(value: unknown, index: number): Villa | null {
  if (!isRecord(value)) return null;
  const room = value as RawRoom;
  const fallback = fallbackVillas[index % fallbackVillas.length];
  const roomType = getRoomTypeName(room.roomType, value.roomTypeDisplayName);
  const amenities = normalizeAmenities(room.amenities);
  const pricePerNight = toNumber(room.pricePerNight, fallback.pricePerNight);
  const roomNumber = typeof room.roomNumber === 'string' ? room.roomNumber : fallback.roomNumber;
  const id = typeof room.id === 'number' ? room.id : fallback.id + index;

  return {
    id,
    roomNumber,
    roomType,
    name: roomType.includes('Villa') ? roomType : `${roomType} Villa`,
    description:
      typeof room.description === 'string' && room.description.length > 0
        ? room.description
        : fallback.description,
    imageUrl: getImageUrl(room.imageUrl, fallbackImages[index % fallbackImages.length]),
    pricePerNight,
    capacity: toNumber(room.capacity, fallback.capacity),
    bedrooms: toNumber(room.bedrooms, fallback.bedrooms),
    isAvailable: typeof room.isAvailable === 'boolean' ? room.isAvailable : true,
    amenities: amenities.length > 0 ? amenities : fallback.amenities,
    rating: toNumber(room.avgRating, fallback.rating),
    reviewCount: toNumber(room.reviewCount, fallback.reviewCount),
    promotions: (Array.isArray(room.promotions) ? room.promotions : []) as VillaPromotion[],
  };
}

export type VillaFiltersParams = {
  checkIn?: string;
  checkOut?: string;
  capacity?: string;
  bedrooms?: string;
  minPrice?: string;
  maxPrice?: string;
  roomTypeId?: string;
  amenities?: string;
  promotion?: string;
};

function buildQuery(params: VillaFiltersParams = {}): string {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([key, val]) => {
    if (val != null && val !== '') sp.append(key, val);
  });
  const qs = sp.toString();
  return qs ? `?${qs}` : '';
}

export async function fetchVillas(filters: VillaFiltersParams = {}): Promise<Villa[]> {
  const data = await api.get<RoomListResponse>(`${API_PATHS.rooms}${buildQuery(filters)}`);
  const rooms = Array.isArray(data?.rooms) ? data.rooms : [];
  const villas = rooms
    .map((item, index) => normalizeRoom(item, index))
    .filter((item): item is Villa => item !== null);
  return villas.length > 0 ? villas : fallbackVillas;
}

export async function fetchVillaById(id: string | number): Promise<Villa | null> {
  const data = await api.get<RoomDetailResponse>(API_PATHS.room(id));
  const room = data?.room;
  if (!room) return null;
  const villa = normalizeRoom(room, 0);
  if (!villa) return null;
  return {
    ...villa,
    rating: typeof data?.avgRating === 'number' ? data.avgRating : villa.rating,
    reviewCount: typeof data?.reviewCount === 'number' ? data.reviewCount : villa.reviewCount,
  };
}

export async function fetchBookedDates(roomId: string | number) {
  return api.get<Array<{ checkIn: string; checkOut: string; status: string }>>(
    API_PATHS.roomBookedDates(roomId)
  );
}