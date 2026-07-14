package de.fliplearn.backend.study.entity;

public enum StudyMode {

    /*
     * Nur aktuell fällige Karten.
     * Das ist dein bisheriger DUE-Modus.
     */
    DUE,

    /*
     * Alle Karten des Lernsets.
     */
    ALL,

    /*
     * Alle Karten in zufälliger Reihenfolge.
     */
    RANDOM,

    /*
     * Nur favorisierte Karten.
     */
    FAVORITES,

    /*
     * Schwierige Karten zuerst.
     */
    DIFFICULT,

    /*
     * Nur Karten, die zuletzt falsch oder
     * als schwierig bewertet wurden.
     */
    WRONG_ONLY,

    /*
     * Nur Karten, die noch nie bewertet wurden.
     */
    NEW_ONLY,

    /*
     * Nur fällige Karten.
     * Funktional ähnlich zu DUE.
     */
    DUE_ONLY,

    /*
     * Nur favorisierte Karten, die gleichzeitig fällig sind.
     */
    FAVORITES_DUE,

    /*
     * Lange Sitzung mit allen verfügbaren Karten.
     */
    MARATHON,

    /*
     * Fünf Sekunden pro Karte.
     * Die Zeitsteuerung läuft hauptsächlich im Frontend.
     */
    LIGHTNING,

    /*
     * Prüfungsmodus ohne sofortige Bewertung.
     */
    EXAM
}