package dev.trinitychurch.lyrics.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SetRepositoryTest : FunSpec({

    lateinit var driver: JdbcSqliteDriver
    lateinit var database: TrinityLyricsDatabase
    lateinit var repo: SetRepository
    lateinit var songRepo: SongRepository

    beforeEach {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TrinityLyricsDatabase.Schema.create(driver)
        database = TrinityLyricsDatabase(driver)
        repo = SetRepository(database)
        songRepo = SongRepository(database, driver)
    }

    afterEach {
        driver.close()
    }

    fun song(id: String) = Song(
        id = id,
        title = "Song $id",
        artist = "",
        sections = listOf(
            SongSection(id = "$id-s", songId = id, label = "V1", type = SectionType.VERSE, body = "...", sortOrder = 1)
        )
    )

    fun serviceSet(id: String = "set-1", items: List<SetItem> = emptyList()) = ServiceSet(
        id = id,
        name = "Sunday Service",
        items = items
    )

    fun item(id: String, setId: String, songId: String, sortOrder: Int) =
        SetItem(id = id, setId = setId, songId = songId, sortOrder = sortOrder)

    test("create set → allSets() emits it") {
        runBlocking {
            repo.createSet(serviceSet())
            val sets = repo.allSets().first()
            sets shouldHaveSize 1
            sets[0].name shouldBe "Sunday Service"
        }
    }

    test("create set + add 3 items → setById() emits correct order") {
        runBlocking {
            songRepo.insert(song("s1"))
            songRepo.insert(song("s2"))
            songRepo.insert(song("s3"))

            repo.createSet(serviceSet("set-1"))
            repo.addItem(item("i1", "set-1", "s1", 0))
            repo.addItem(item("i2", "set-1", "s2", 1))
            repo.addItem(item("i3", "set-1", "s3", 2))

            val result = repo.setById("set-1").first()!!
            result.items shouldHaveSize 3
            result.items[0].songId shouldBe "s1"
            result.items[1].songId shouldBe "s2"
            result.items[2].songId shouldBe "s3"
        }
    }

    test("reorderItems → emitted set reflects new sort_order") {
        runBlocking {
            songRepo.insert(song("s1"))
            songRepo.insert(song("s2"))
            songRepo.insert(song("s3"))

            repo.createSet(serviceSet("set-1"))
            repo.addItem(item("i1", "set-1", "s1", 0))
            repo.addItem(item("i2", "set-1", "s2", 1))
            repo.addItem(item("i3", "set-1", "s3", 2))

            repo.reorderItems("set-1", listOf("i3", "i2", "i1"))

            val result = repo.setById("set-1").first()!!
            result.items[0].songId shouldBe "s3"
            result.items[1].songId shouldBe "s2"
            result.items[2].songId shouldBe "s1"
        }
    }

    test("removeItem → item gone from set, song remains in songs table") {
        runBlocking {
            songRepo.insert(song("s1"))
            repo.createSet(serviceSet("set-1"))
            repo.addItem(item("i1", "set-1", "s1", 0))

            repo.removeItem("i1")

            val result = repo.setById("set-1").first()!!
            result.items.shouldBeEmpty()

            val songs = songRepo.allSongs().first()
            songs shouldHaveSize 1
        }
    }

    test("deleteSet → removed from allSets()") {
        runBlocking {
            repo.createSet(serviceSet())
            repo.deleteSet("set-1")
            repo.allSets().first().shouldBeEmpty()
        }
    }
})
