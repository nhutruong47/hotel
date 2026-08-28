package com.hsf.hotel.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.io.FileWriter;
import java.io.PrintWriter;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter("d:/hotelNew/hotel/error.log", true))) {
            pw.println("=== ERROR at " + java.time.LocalDateTime.now() + " ===");
            e.printStackTrace(pw);
            pw.println("=== END ===");
        }
        throw e; // re-throw to let Spring handle it normally
    }
}
