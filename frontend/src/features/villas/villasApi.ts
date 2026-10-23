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
  slug?: string;
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
  galleryImages?: string[];
  houseRules?: string;
  policies?: string;
  nearbyAttractions?: string;
  checkInTime?: string;
  checkOutTime?: string;
  latitude?: number;
  longitude?: number;
  nearbyRestaurants?: string;
  nearbyCafes?: string;
  nearbyAirport?: string;
  directions?: string;
  minimumStay?: number;
  maximumStay?: number;
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

function getVillaTitle(roomNumber: string, slug: string | undefined, roomType: string): string {
  const code = roomNumber.toUpperCase();
  if (code === 'GV-01') return 'Garden Pool Villa';
  if (code === 'TF-02') return 'Terrace Family Villa';
  if (code === 'HR-03') return 'Hillside Residence';
  if (code === 'SE-04') return 'Signature Four Bedroom Estate';
  if (code === 'WC-05' || code === 'WC-04' || slug === 'water-courtyard-villa') return 'Water Courtyard Villa';
  if (code === 'FP-06' || code === 'FP-05' || slug === 'forest-pavilion') return 'Forest Pavilion';
  return roomType.includes('Villa') || roomType.includes('Residence') || roomType.includes('Pavilion')
    ? roomType
    : `${roomType} Villa`;
}

function normalizeRoom(value: unknown, index: number): Villa | null {
  if (!isRecord(value)) return null;
  const room = value as RawRoom;
  const fallback = fallbackVillas[index % fallbackVillas.length];
  const roomType = getRoomTypeName(room.roomType, value.roomTypeDisplayName);
  const amenities = normalizeAmenities(room.amenities);
  const pricePerNight = toNumber(room.pricePerNight, fallback.pricePerNight);
  const roomNumber = typeof room.roomNumber === 'string' ? room.roomNumber : fallback.roomNumber;
  const slug = typeof room.slug === 'string' && room.slug.length > 0 ? room.slug : fallback.slug;
  const id = typeof room.id === 'number' ? room.id : fallback.id + index;
  const galleryImages = Array.isArray(room.galleryImages)
    ? room.galleryImages.filter((img): img is string => typeof img === 'string' && img.length > 0)
    : undefined;

  return {
    id,
    slug,
    roomNumber,
    roomType,
    name: getVillaTitle(roomNumber, slug, roomType),
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
    galleryImages,
    houseRules: typeof room.houseRules === 'string' ? room.houseRules : undefined,
    policies: typeof room.policies === 'string' ? room.policies : undefined,
    nearbyAttractions: typeof room.nearbyAttractions === 'string' ? room.nearbyAttractions : undefined,
    checkInTime: typeof room.checkInTime === 'string' ? room.checkInTime : undefined,
    checkOutTime: typeof room.checkOutTime === 'string' ? room.checkOutTime : undefined,
    latitude: typeof room.latitude === 'number' ? room.latitude : undefined,
    longitude: typeof room.longitude === 'number' ? room.longitude : undefined,
    nearbyRestaurants: typeof room.nearbyRestaurants === 'string' ? room.nearbyRestaurants : undefined,
    nearbyCafes: typeof room.nearbyCafes === 'string' ? room.nearbyCafes : undefined,
    nearbyAirport: typeof room.nearbyAirport === 'string' ? room.nearbyAirport : undefined,
    directions: typeof room.directions === 'string' ? room.directions : undefined,
    minimumStay: typeof room.minimumStay === 'number' ? room.minimumStay : undefined,
    maximumStay: typeof room.maximumStay === 'number' ? room.maximumStay : undefined,
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
  page?: string;
  size?: string;
  sort?: 'recommended' | 'price-asc' | 'price-desc' | 'capacity-desc' | 'rating-desc';
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
  // The current catalogue applies its final UI filters locally, so request the
  // largest bounded API page while the server contract remains paginated.
  const data = await api.get<RoomListResponse>(`${API_PATHS.rooms}${buildQuery({ size: '100', ...filters })}`);
  const rooms = Array.isArray(data?.rooms) ? data.rooms : [];
  const villas = rooms
    .map((item, index) => normalizeRoom(item, index))
    .filter((item): item is Villa => item !== null);
  return villas;
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
