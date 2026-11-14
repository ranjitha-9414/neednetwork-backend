
package com.example.neednetwork.controller;

import com.example.neednetwork.dto.HomeStatisticsResponse;
import com.example.neednetwork.service.HomeStatisticsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeStatisticsController {

    private final HomeStatisticsService statisticsService;

    public HomeStatisticsController(
            HomeStatisticsService statisticsService) {

        this.statisticsService = statisticsService;
    }

    // Get dynamic homepage statistics
    @GetMapping("/statistics")
    public HomeStatisticsResponse getStatistics() {
        return statisticsService.getStatistics();
    }
}
