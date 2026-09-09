package com.hsf.hotel.room.dto;

import lombok.Data;

@Data
public class RoomTypeDTO implements java.io.Serializable {
    private Integer id;
    private String name;
    private String displayName;
    private String description;
}
