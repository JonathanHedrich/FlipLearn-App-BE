package de.fliplearn.backend.statistics.controller;

import de.fliplearn.backend.statistics.dto.StatisticsOverviewResponse;
import de.fliplearn.backend.statistics.service.StatisticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(
            StatisticsService statisticsService
    ) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public StatisticsOverviewResponse getOverview(
            Authentication authentication
    ) {
        return statisticsService.getOverview(
                authentication.getName()
        );
    }
}