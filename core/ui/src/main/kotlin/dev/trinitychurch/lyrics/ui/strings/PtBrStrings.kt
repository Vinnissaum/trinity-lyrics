package dev.trinitychurch.lyrics.ui.strings

object PtBrStrings : StringResources {
    // Common
    override val appName = "Trinity Lyrics"
    override val ok = "OK"
    override val cancel = "Cancelar"
    override val save = "Salvar"
    override val delete = "Excluir"
    override val edit = "Editar"
    override val search = "Pesquisar"
    override val settings = "Configurações"
    override val back = "Voltar"
    override val add = "Adicionar"
    override val confirm = "Confirmar"
    override val close = "Fechar"
    override val error = "Erro"

    // Language picker
    override val chooseLanguage = "Escolha o idioma"
    override val chooseLanguageSubtitle = "Você pode alterar isso nas configurações a qualquer momento."
    override val continueButton = "Continuar"
    override val portuguese = "Português (Brasil)"
    override val english = "English"

    // Library
    override val songs = "Músicas"
    override val noSongsFound = "Nenhuma música encontrada"
    override val searchHint = "Pesquisar músicas..."
    override val newSong = "Nova Música"
    override val importSongs = "Importar"
    override val deleteConfirm = "Excluir esta música?"

    // Editor
    override val songTitle = "Título"
    override val artist = "Artista"
    override val sections = "Seções"
    override val addSection = "Adicionar seção"
    override val sectionType = "Tipo"
    override val sectionLabel = "Rótulo"
    override val sectionBody = "Letra"
    override val livePreview = "Pré-visualização"

    // Section types
    override val verse = "Estrofe"
    override val chorus = "Refrão"
    override val bridge = "Ponte"
    override val preChorus = "Pré-refrão"
    override val outro = "Final"
    override val interlude = "Interlúdio"
    override val tag = "Tag"

    // Set builder
    override val serviceSets = "Cultos"
    override val newSet = "Novo Culto"
    override val setName = "Nome do culto"
    override val serviceDate = "Data"
    override val addSong = "Adicionar Música"
    override val removeSong = "Remover"
    override val startPresentation = "Iniciar Apresentação"
    override val setEmpty = "Adicione músicas ao culto primeiro"

    // Import
    override val importHolyrics = "Importar do Holyrics"
    override val selectFile = "Selecionar Arquivo"
    override val foundSongs = "músicas encontradas"
    override val importing = "Importando..."
    override val importComplete = "Importação concluída"
    override val importError = "Erro ao importar"
    override val duplicateFound = "Duplicata encontrada"
    override val skip = "Ignorar"
    override val overwrite = "Substituir"
    override val plainTextImport = "Texto Puro"
    override val pasteLyrics = "Cole a letra aqui..."

    // Presentation
    override val currentSlide = "Slide atual"
    override val nextSlide = "Próximo slide"
    override val blank = "Tela em branco"
    override val freeze = "Congelar"
    override val exitPresentation = "Encerrar apresentação"
    override val slideOf = "de"
    override val songOf = "de"

    // Settings
    override val targetMonitor = "Monitor de apresentação"
    override val fontSize = "Tamanho da fonte"
    override val maxLinesPerSlide = "Linhas por slide"
    override val language = "Idioma"

    // Errors
    override val fileMalformed = "Arquivo inválido ou corrompido"
    override val noSectionsInSong = "A música precisa ter ao menos uma seção"
    override val titleRequired = "O título é obrigatório"
}
