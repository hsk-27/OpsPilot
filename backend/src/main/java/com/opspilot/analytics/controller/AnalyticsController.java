package com.opspilot.analytics.controller;

import com.opspilot.analytics.dto.OverviewAnalyticsDto;
import com.opspilot.analytics.service.AnalyticsService;
import com.opspilot.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics & KPIs", description = "Operational accounts payable KPIs, workflow cycle times, and AI telemetry")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get high-level operational AP metrics")
    public ResponseEntity<ApiResponse<OverviewAnalyticsDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOverview()));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Get invoice counts by processing status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoiceBreakdown() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getInvoiceBreakdown()));
    }
}
