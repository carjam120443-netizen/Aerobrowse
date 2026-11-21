package com.example.data.local

import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun ensureSeeded() {
        if (bookmarkDao.countBookmarks() == 0) {
            val now = System.currentTimeMillis()
            bookmarkDao.insertAll(
                listOf(
                    BookmarkEntity(
                        title = "DuckDuckGo",
                        url = "https://duckduckgo.com/",
                        category = "Search",
                        createdAt = now + 5,
                    ),
                    BookmarkEntity(
                        title = "DuckDuckGo Lite",
                        url = "https://lite.duckduckgo.com/lite/",
                        category = "Search",
                        createdAt = now + 4,
                    ),
                    BookmarkEntity(
                        title = "EFF Cover Your Tracks",
                        url = "https://coveryourtracks.eff.org/",
                        category = "Privacy",
                        createdAt = now + 3,
                    ),
                    BookmarkEntity(
                        title = "Wikipedia",
                        url = "https://www.wikipedia.org/",
                        category = "Knowledge",
                        createdAt = now + 2,
                    ),
                    BookmarkEntity(
                        title = "Internet Archive",
                        url = "https://archive.org/",
                        category = "Knowledge",
                        createdAt = now + 1,
                    ),
                    BookmarkEntity(
                        title = "Proton",
                        url = "https://proton.me/",
                        category = "Privacy",
                        createdAt = now,
                    ),
                )
            )
        }
    }

    suspend fun addBookmark(title: String, url: String, category: String = "Saved") {
        val cleanTitle = title.ifBlank { url }
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                title = cleanTitle,
                url = url,
                category = category,
            )
        )
    }

    suspend fun removeById(id: Int) = bookmarkDao.deleteById(id)

    suspend fun removeByUrl(url: String) = bookmarkDao.deleteByUrl(url)
}
