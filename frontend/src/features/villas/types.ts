export type VillaAmenity = {
  id?: number;
  name: string;
};

export type VillaPromotion = {
  id: number;
  title: string;
  promoCode?: string;
  discountPercent?: number;
  discountAmount?: number;
};

export type Villa = {
  id: number;
  name: string;
  roomNumber: string;
  roomType: string;
  description: string;
  imageUrl: string;
  pricePerNight: number;
  capacity: number;
  bedrooms: number;
  isAvailable: boolean;
  amenities: VillaAmenity[];
  rating: number;
  reviewCount: number;
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
  promotions?: VillaPromotion[];
};

export type SortOption = 'recommended' | 'price-asc' | 'price-desc' | 'capacity-desc';

export type VillaFilters = {
  query: string;
  roomType: string;
  minPrice: string;
  maxPrice: string;
  amenity: string;
  sort: SortOption;
  checkIn?: string;
  checkOut?: string;
  adults?: string;
  children?: string;
  bedrooms?: string;
  promotion?: string;
};
