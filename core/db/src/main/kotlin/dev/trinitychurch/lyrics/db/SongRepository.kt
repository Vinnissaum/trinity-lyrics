package dev.trinitychurch.lyrics.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import dev.trinitychurch.lyrics.domain.SectionType
import dev.trinitychurch.lyrics.domain.Song
import dev.trinitychurch.lyrics.domain.SongSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SongRepository(
    private val db: TrinityLyricsDatabase,
    private val driver: SqlDriver
) {

    fun allSongs(): Flow<List<Song>> =
        db.trinityLyricsQueries.selectAllNonDeleted()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { loadSongWithSections(it) } }

    fun songById(id: String): Flow<Song?> =
        db.trinityLyricsQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { row -> row?.let { loadSongWithSections(it) } }

    fun search(query: String): Flow<List<Song>> =
        allSongs().map { songs ->
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.sections.any { it.body.contains(query, ignoreCase = true) }
            }
        }

    suspend fun insert(song: Song) = withContext(Dispatchers.IO) {
        db.transaction {
            insertSongRow(song)
            song.sections.forEach { insertSectionRow(it) }
            insertFts(song)
        }
    }

    suspend fun update(song: Song) = withContext(Dispatchers.IO) {
        db.transaction {
            db.trinityLyricsQueries.updateSong(
                title = song.title,
                artist = song.artist,
                source = song.source,
                slideConfig = null,
                updatedAt = System.currentTimeMillis(),
                id = song.id
            )
            db.trinityLyricsQueries.deleteSectionsBySongId(song.id)
            song.sections.forEach { insertSectionRow(it) }
            deleteFts(song.id)
            insertFts(song)
        }
    }

    suspend fun softDelete(id: String) = withContext(Dispatchers.IO) {
        db.transaction {
            db.trinityLyricsQueries.softDeleteSong(
                deletedAt = System.currentTimeMillis(),
                id = id
            )
            deleteFts(id)
        }
    }

    suspend fun insertAll(songs: List<Song>, source: String) = withContext(Dispatchers.IO) {
        db.transaction {
            songs.forEach { song ->
                val withSource = song.copy(source = source)
                insertSongRow(withSource)
                withSource.sections.forEach { insertSectionRow(it) }
                insertFts(withSource)
            }
        }
    }

    private fun insertSongRow(song: Song) {
        db.trinityLyricsQueries.insertSong(
            id = song.id,
            title = song.title,
            artist = song.artist,
            source = song.source,
            slideConfig = null,
            createdAt = song.createdAt,
            updatedAt = song.updatedAt
        )
    }

    private fun insertSectionRow(section: SongSection) {
        db.trinityLyricsQueries.insertSection(
            id = section.id,
            songId = section.songId,
            label = section.label,
            type = section.type.name,
            body = section.body,
            sortOrder = section.sortOrder.toLong()
        )
    }

    private fun insertFts(song: Song) {
        val body = song.sections.joinToString("\n") { it.body }
        driver.execute(
            identifier = null,
            sql = "INSERT INTO songs_fts(song_id, title, artist, body) VALUES (?, ?, ?, ?)",
            parameters = 4
        ) {
            bindString(0, song.id)
            bindString(1, song.title)
            bindString(2, song.artist)
            bindString(3, body)
        }
    }

    private fun deleteFts(songId: String) {
        driver.execute(
            identifier = null,
            sql = "DELETE FROM songs_fts WHERE song_id = ?",
            parameters = 1
        ) {
            bindString(0, songId)
        }
    }

    private fun loadSongWithSections(row: Songs): Song {
        val sections = db.trinityLyricsQueries.selectSectionsBySongId(row.id)
            .executeAsList()
            .map { s ->
                SongSection(
                    id = s.id,
                    songId = s.song_id,
                    label = s.label,
                    type = SectionType.valueOf(s.type),
                    body = s.body,
                    sortOrder = s.sort_order.toInt()
                )
            }
        return Song(
            id = row.id,
            title = row.title,
            artist = row.artist,
            sections = sections,
            source = row.source,
            createdAt = row.created_at,
            updatedAt = row.updated_at
        )
    }
}
