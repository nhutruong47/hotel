package com.hsf.hotel.dto;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoomMapper.class})
public interface BookingMapper {
    BookingMapper INSTANCE = Mappers.getMapper(BookingMapper.class);

    @Mapping(target = "user", source = "user", qualifiedByName = "userToUserSummaryDTO")
    @Mapping(target = "approvedBy", source = "approvedBy", qualifiedByName = "userToUserSummaryDTO")
    @Mapping(target = "statusDisplayName", source = "status.displayName")
    BookingDTO bookingToBookingDTO(Booking booking);

    List<BookingDTO> bookingsToBookingDTOs(List<Booking> bookings);

    @Named("userToUserSummaryDTO")
    default UserSummaryDTO userToUserSummaryDTO(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryDTO.from(user);
    }
}
