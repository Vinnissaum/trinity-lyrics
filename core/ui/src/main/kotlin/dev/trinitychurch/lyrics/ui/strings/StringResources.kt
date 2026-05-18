package dev.trinitychurch.lyrics.ui.strings

interface StringResources {
    // Common
    val appName: String
    val ok: String
    val cancel: String
    val save: String
    val delete: String
    val edit: String
    val search: String
    val settings: String
    val back: String
    val add: String
    val confirm: String
    val close: String
    val error: String

    // Language picker
    val chooseLanguage: String
    val chooseLanguageSubtitle: String
    val continueButton: String
    val portuguese: String
    val english: String

    // Library
    val songs: String
    val noSongsFound: String
    val searchHint: String
    val newSong: String
    val importSongs: String
    val deleteConfirm: String

    // Editor
    val songTitle: String
    val artist: String
    val sections: String
    val addSection: String
    val sectionType: String
    val sectionLabel: String
    val sectionBody: String
    val livePreview: String

    // Section types
    val verse: String
    val chorus: String
    val bridge: String
    val preChorus: String
    val outro: String
    val interlude: String
    val tag: String

    // Set builder
    val serviceSets: String
    val newSet: String
    val setName: String
    val serviceDate: String
    val addSong: String
    val removeSong: String
    val startPresentation: String
    val setEmpty: String

    // Import
    val importHolyrics: String
    val selectFile: String
    val foundSongs: String
    val importing: String
    val importComplete: String
    val importError: String
    val duplicateFound: String
    val skip: String
    val overwrite: String
    val plainTextImport: String
    val pasteLyrics: String

    // Presentation
    val currentSlide: String
    val nextSlide: String
    val blank: String
    val freeze: String
    val exitPresentation: String
    val slideOf: String
    val songOf: String

    // Settings
    val targetMonitor: String
    val fontSize: String
    val maxLinesPerSlide: String
    val language: String

    // Errors
    val fileMalformed: String
    val noSectionsInSong: String
    val titleRequired: String
}
