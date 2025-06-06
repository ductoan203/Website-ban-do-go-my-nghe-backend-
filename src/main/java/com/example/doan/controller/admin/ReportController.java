package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.response.ReportResponse;
import com.example.doan.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ApiResponse<ReportResponse> getReport(@RequestParam(defaultValue = "WEEK") String timeRange) {
        return ApiResponse.<ReportResponse>builder()
                .result(reportService.getReportData(timeRange.toUpperCase()))
                .build();
    }

}
