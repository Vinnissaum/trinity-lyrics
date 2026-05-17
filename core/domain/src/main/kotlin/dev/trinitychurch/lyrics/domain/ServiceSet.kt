package dev.trinitychurch.lyrics.domain

data class ServiceSet(
    val id: String,
    val name: String,
    val serviceDate: String? = null,
    val items: List<SetItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
