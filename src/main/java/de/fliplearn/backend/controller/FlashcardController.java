package de.fliplearn.backend.controller;

import de.fliplearn.backend.dto.CreateFlashcardRequest;
import de.fliplearn.backend.dto.FlashcardResponse;
import de.fliplearn.backend.dto.UpdateFlashcardRequest;
import de.fliplearn.backend.service.FlashcardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sets/{setId}/cards")
public class FlashcardController {

    private final FlashcardService flashcardService;

    public FlashcardController(
            FlashcardService flashcardService
    ) {
        this.flashcardService = flashcardService;
    }

    @GetMapping
    public List<FlashcardResponse> getCards(
            @PathVariable Long setId,
            Authentication authentication
    ) {
        return flashcardService.getCards(
                setId,
                authentication.getName()
        );
    }

    @GetMapping("/{cardId}")
    public FlashcardResponse getCard(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            Authentication authentication
    ) {
        return flashcardService.getCard(
                setId,
                cardId,
                authentication.getName()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlashcardResponse createCard(
            @PathVariable Long setId,
            @Valid @RequestBody CreateFlashcardRequest request,
            Authentication authentication
    ) {
        return flashcardService.createCard(
                setId,
                request,
                authentication.getName()
        );
    }

    @PutMapping("/{cardId}")
    public FlashcardResponse updateCard(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateFlashcardRequest request,
            Authentication authentication
    ) {
        return flashcardService.updateCard(
                setId,
                cardId,
                request,
                authentication.getName()
        );
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(
            @PathVariable Long setId,
            @PathVariable Long cardId,
            Authentication authentication
    ) {
        flashcardService.deleteCard(
                setId,
                cardId,
                authentication.getName()
        );
    }
}