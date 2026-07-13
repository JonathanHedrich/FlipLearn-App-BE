package de.fliplearn.backend.study.controller;

import de.fliplearn.backend.study.dto.StartStudySessionRequest;
import de.fliplearn.backend.study.dto.StudyReviewResponse;
import de.fliplearn.backend.study.dto.StudySessionResponse;
import de.fliplearn.backend.study.dto.SubmitReviewRequest;
import de.fliplearn.backend.study.service.StudyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyService studyService;

    public StudyController(
            StudyService studyService
    ) {
        this.studyService = studyService;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse startSession(
            @Valid
            @RequestBody
            StartStudySessionRequest request,
            Authentication authentication
    ) {
        return studyService.startSession(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/sessions/{sessionId}")
    public StudySessionResponse getSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return studyService.getSession(
                sessionId,
                authentication.getName()
        );
    }

    @PostMapping(
            "/sessions/{sessionId}/reviews"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public StudyReviewResponse submitReview(
            @PathVariable Long sessionId,
            @Valid
            @RequestBody
            SubmitReviewRequest request,
            Authentication authentication
    ) {
        return studyService.submitReview(
                sessionId,
                request,
                authentication.getName()
        );
    }
}