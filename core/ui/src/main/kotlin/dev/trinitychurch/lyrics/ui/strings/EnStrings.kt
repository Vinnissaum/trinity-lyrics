package dev.trinitychurch.lyrics.ui.strings

object EnStrings : StringResources {
    // Common
    override val appName = "Trinity Lyrics"
    override val ok = "OK"
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val edit = "Edit"
    override val search = "Search"
    override val settings = "Settings"
    override val back = "Back"
    override val add = "Add"
    override val confirm = "Confirm"
    override val close = "Close"
    override val error = "Error"

    // Language picker
    override val chooseLanguage = "Choose language"
    override val chooseLanguageSubtitle = "You can change this in settings at any time."
    override val continueButton = "Continue"
    override val portuguese = "Português (Brasil)"
    override val english = "English"

    // Library
    override val songs = "Songs"
    override val noSongsFound = "No songs found"
    override val searchHint = "Search songs..."
    override val newSong = "New Song"
    override val importSongs = "Import"
    override val deleteConfirm = "Delete this song?"

    // Editor
    override val songTitle = "Title"
    override val artist = "Artist"
    override val sections = "Sections"
    override val addSection = "Add section"
    override val sectionType = "Type"
    override val sectionLabel = "Label"
    override val sectionBody = "Lyrics"
    override val livePreview = "Live preview"

    // Section types
    override val verse = "Verse"
    override val chorus = "Chorus"
    override val bridge = "Bridge"
    override val preChorus = "Pre-Chorus"
    override val outro = "Outro"
    override val interlude = "Interlude"
    override val tag = "Tag"

    // Set builder
    override val serviceSets = "Service Sets"
    override val newSet = "New Set"
    override val setName = "Set name"
    override val serviceDate = "Date"
    override val addSong = "Add Song"
    override val removeSong = "Remove"
    override val startPresentation = "Start Presentation"
    override val setEmpty = "Add songs to the set first"

    // Import
    override val importHolyrics = "Import from Holyrics"
    override val selectFile = "Select File"
    override val foundSongs = "songs found"
    override val importing = "Importing..."
    override val importComplete = "Import complete"
    override val importError = "Import error"
    override val duplicateFound = "Duplicate found"
    override val skip = "Skip"
    override val overwrite = "Overwrite"
    override val plainTextImport = "Plain Text"
    override val pasteLyrics = "Paste lyrics here..."

    // Presentation
    override val currentSlide = "Current slide"
    override val nextSlide = "Next slide"
    override val blank = "Blank screen"
    override val freeze = "Freeze"
    override val exitPresentation = "Exit presentation"
    override val slideOf = "of"
    override val songOf = "of"

    // Settings
    override val targetMonitor = "Presentation monitor"
    override val fontSize = "Font size"
    override val maxLinesPerSlide = "Lines per slide"
    override val language = "Language"

    // Errors
    override val fileMalformed = "Invalid or corrupted file"
    override val noSectionsInSong = "The song must have at least one section"
    override val titleRequired = "Title is required"
}
