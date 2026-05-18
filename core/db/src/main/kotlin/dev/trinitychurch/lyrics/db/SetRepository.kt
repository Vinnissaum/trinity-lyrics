package dev.trinitychurch.lyrics.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.trinitychurch.lyrics.domain.ServiceSet
import dev.trinitychurch.lyrics.domain.SetItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SetRepository(private val db: TrinityLyricsDatabase) {

    fun allSets(): Flow<List<ServiceSet>> =
        db.trinityLyricsQueries.selectAllSets()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { loadSetWithItems(it) } }

    fun setById(id: String): Flow<ServiceSet?> =
        db.trinityLyricsQueries.selectSetById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { row -> row?.let { loadSetWithItems(it) } }

    suspend fun createSet(set: ServiceSet) = withContext(Dispatchers.IO) {
        db.transaction {
            db.trinityLyricsQueries.insertSet(
                id = set.id,
                name = set.name,
                serviceDate = set.serviceDate,
                createdAt = set.createdAt,
                updatedAt = set.updatedAt
            )
            set.items.forEach { insertItemRow(it) }
        }
    }

    suspend fun updateSet(set: ServiceSet) = withContext(Dispatchers.IO) {
        db.trinityLyricsQueries.updateSet(
            name = set.name,
            serviceDate = set.serviceDate,
            updatedAt = System.currentTimeMillis(),
            id = set.id
        )
    }

    suspend fun deleteSet(id: String) = withContext(Dispatchers.IO) {
        db.transaction {
            val items = db.trinityLyricsQueries.selectItemsBySetId(id).executeAsList()
            items.forEach { db.trinityLyricsQueries.deleteSetItem(it.id) }
            db.trinityLyricsQueries.deleteSet(id)
        }
    }

    suspend fun addItem(item: SetItem) = withContext(Dispatchers.IO) {
        insertItemRow(item)
    }

    suspend fun removeItem(id: String) = withContext(Dispatchers.IO) {
        db.trinityLyricsQueries.deleteSetItem(id)
    }

    suspend fun reorderItems(setId: String, orderedIds: List<String>) = withContext(Dispatchers.IO) {
        db.transaction {
            orderedIds.forEachIndexed { index, id ->
                db.trinityLyricsQueries.updateSetItemOrder(sortOrder = index.toLong(), id = id)
            }
        }
    }

    private fun insertItemRow(item: SetItem) {
        db.trinityLyricsQueries.insertSetItem(
            id = item.id,
            setId = item.setId,
            songId = item.songId,
            sortOrder = item.sortOrder.toLong()
        )
    }

    private fun loadSetWithItems(row: Sets): ServiceSet {
        val items = db.trinityLyricsQueries.selectItemsBySetId(row.id)
            .executeAsList()
            .mapNotNull { r ->
                val songId = r.song_id ?: return@mapNotNull null
                SetItem(
                    id = r.id,
                    setId = r.set_id,
                    songId = songId,
                    sortOrder = r.sort_order.toInt()
                )
            }
        return ServiceSet(
            id = row.id,
            name = row.name,
            serviceDate = row.service_date,
            items = items,
            createdAt = row.created_at,
            updatedAt = row.updated_at
        )
    }
}
