package dev.trinitychurch.lyrics.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SongRepositoryTest : FunSpec({

    lateinit var driver: JdbcSqliteDriver
    lateinit var database: TrinityLyricsDatabase
    lateinit var repo: SongRepository

    beforeEach {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrinityLyricsDatabase.Schema.create(driver)
        database = TrinityLyricsDatabase(driver)
        repo = SongRepository(database, driver)
    }

    afterEach {
        driver.close()
    }

    fun song(
        id: String = "song-1",
        title: String = "Test Song",
        artist: String = "Test Artist",
        body: String = "Some lyrics here"
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        sections = listOf(
            SongSection(
                id = "$id-sec",
                songId = id,
                label = "Verso 1",
                type = SectionType.VERSE,
                body = body,
                sortOrder = 1
            )
        )
    )

    test("insert song → allSongs() emits it") {
        runBlocking {
            repo.insert(song())
            val songs = repo.allSongs().first()
            songs shouldHaveSize 1
            songs[0].title shouldBe "Test Song"
        }
    }

    test("insert song with sections → sections are loaded") {
        runBlocking {
            repo.insert(song())
            val songs = repo.allSongs().first()
            songs[0].sections shouldHaveSize 1
            songs[0].sections[0].body shouldBe "Some lyrics here"
        }
    }

    test("search by body text → song appears in results") {
        runBlocking {
            repo.insert(song(body = "SENHOR É MEU PASTOR"))
            val results = repo.search("PASTOR").first()
            results shouldHaveSize 1
            results[0].title shouldBe "Test Song"
        }
    }

    test("search by title → song appears in results") {
        runBlocking {
            repo.insert(song(title = "Aleluia"))
            val results = repo.search("Alelu").first()
            results shouldHaveSize 1
        }
    }

    test("softDelete → song absent from allSongs(), present with deleted_at set") {
        runBlocking {
            repo.insert(song())
            repo.softDelete("song-1")
            val songs = repo.allSongs().first()
            songs.shouldBeEmpty()

            val rawRow = database.trinityLyricsQueries.selectById("song-1").executeAsOneOrNull()
            rawRow.shouldNotBeNull()
            rawRow.deleted_at shouldNotBe null
        }
    }

    test("insertAll → all songs appear in allSongs()") {
        runBlocking {
            val songs = listOf(
                song(id = "s1", title = "Song 1"),
                song(id = "s2", title = "Song 2"),
                song(id = "s3", title = "Song 3")
            )
            repo.insertAll(songs, source = "holyrics")
            val result = repo.allSongs().first()
            result shouldHaveSize 3
        }
    }

    test("songById returns correct song") {
        runBlocking {
            repo.insert(song(title = "My Song"))
            val found = repo.songById("song-1").first()
            found.shouldNotBeNull()
            found.title shouldBe "My Song"
        }
    }
})
