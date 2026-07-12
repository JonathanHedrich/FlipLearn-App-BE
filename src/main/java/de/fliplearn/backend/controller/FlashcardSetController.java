package de.fliplearn.backend.controller;

import de.fliplearn.backend.dto.CreateFlashcardSetRequest;
import de.fliplearn.backend.dto.FlashcardSetResponse;
import de.fliplearn.backend.service.FlashcardSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import de.fliplearn.backend.dto.UpdateFlashcardSetRequest;

import java.util.List;

@RestController
@RequestMapping("/api/sets")
public class FlashcardSetController {

    private final FlashcardSetService flashcardSetService;

    public FlashcardSetController(
            FlashcardSetService flashcardSetService
    ) {
        this.flashcardSetService = flashcardSetService;
    }

    @GetMapping
    public List<FlashcardSetResponse> getAllSets(
            Authentication authentication
    ) {
        return flashcardSetService.getAllSets(
                authentication.getName()
        );
    }

    @GetMapping("/{setId}")
    public FlashcardSetResponse getSet(
            @PathVariable Long setId,
            Authentication authentication
    ) {
        return flashcardSetService.getSet(
                setId,
                authentication.getName()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardSetResponse createSet(
            @Valid
            @RequestBody
            CreateFlashcardSetRequest request,
            Authentication authentication
    ) {
        return flashcardSetService.createSet(
                request,
                authentication.getName()
        );
    }
    @PutMapping("/{setId}")
    public FlashcardSetResponse updateSet(
            @PathVariable Long setId,
            @Valid @RequestBody UpdateFlashcardSetRequest request,
            Authentication authentication
    ) {
        return flashcardSetService.updateSet(
                setId,
                request,
                authentication.getName()
        );
    }

    @DeleteMapping("/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(
            @PathVariable Long setId,
            Authentication authentication
    ) {
        flashcardSetService.deleteSet(
                setId,
                authentication.getName()
        );
    }
}