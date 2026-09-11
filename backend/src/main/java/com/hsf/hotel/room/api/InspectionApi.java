package com.hsf.hotel.room.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.ForbiddenException;
import com.hsf.hotel.room.dto.InspectionDto;
import com.hsf.hotel.room.service.VillaInspectionService;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/inspections")
public class InspectionApi {

    private final VillaInspectionService inspectionService;

    public InspectionApi(VillaInspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionDto.InspectionResponse>>> listAll(HttpSession session) {
        requireStaffOrAbove(session);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getAllInspections()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InspectionDto.InspectionStatsResponse>> getStats(HttpSession session) {
        requireStaffOrAbove(session);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getStats()));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<InspectionDto.InspectionResponse>>> forRoom(@PathVariable Integer roomId, HttpSession session) {
        requireStaffOrAbove(session);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getInspectionsForRoom(roomId)));
    }

    @GetMapping("/room/{roomId}/latest")
    public ResponseEntity<ApiResponse<InspectionDto.InspectionResponse>> latestForRoom(@PathVariable Integer roomId, HttpSession session) {
        requireStaffOrAbove(session);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getLatestInspectionForRoom(roomId).orElse(null)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionDto.InspectionResponse>> getById(@PathVariable Integer id, HttpSession session) {
        requireStaffOrAbove(session);
        return ResponseEntity.ok(ApiResponse.ok(inspectionService.getInspectionById(id).orElse(null)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InspectionDto.InspectionResponse>> create(
            @Valid @RequestBody InspectionDto.InspectionRequest req,
            HttpSession session) {
        User user = requireStaffOrAbove(session);
        InspectionDto.InspectionResponse created = inspectionService.createInspection(req, user);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionDto.InspectionResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody InspectionDto.InspectionRequest req,
            HttpSession session) {
        User user = requireStaffOrAbove(session);
        InspectionDto.InspectionResponse updated = inspectionService.updateInspection(id, req, user);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> delete(@PathVariable Integer id, HttpSession session) {
        requireManagerOrAdmin(session);
        inspectionService.deleteInspection(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã xóa bản ghi thanh tra thành công!")));
    }

    private User requireStaffOrAbove(HttpSession session) {
        User u = (User) session.getAttribute("user");
        if (u == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        if (!u.getRoleEnum().includes(UserRole.STAFF)) {
            throw new ForbiddenException("Yêu cầu quyền nhân viên trở lên");
        }
        return u;
    }

    private User requireManagerOrAdmin(HttpSession session) {
        User u = (User) session.getAttribute("user");
        if (u == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        if (!u.getRoleEnum().includes(UserRole.MANAGER)) {
            throw new ForbiddenException("Yêu cầu quyền quản lý trở lên");
        }
        return u;
    }
}
