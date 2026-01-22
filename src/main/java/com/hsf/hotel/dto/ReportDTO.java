package com.hsf.hotel.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReportDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
