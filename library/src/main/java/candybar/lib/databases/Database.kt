package candybar.lib.databases

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import android.util.Log
import candybar.lib.helpers.DrawableHelper
import candybar.lib.helpers.JsonHelper
import candybar.lib.items.Icon
import candybar.lib.items.ImageSize
import candybar.lib.items.Request
import candybar.lib.items.Wallpaper
import candybar.lib.preferences.Preferences
import com.danimahardhika.android.helpers.core.TimeHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.lang.ref.WeakReference
import java.util.Collections

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class Database(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private var sqliteDatabase: SQLiteDatabase? = null

    override fun onCreate(db: SQLiteDatabase) {
        val createTableRequest = "CREATE TABLE IF NOT EXISTS $TABLE_REQUEST (" +
                "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "$KEY_NAME TEXT NOT NULL, " +
                "$KEY_ACTIVITY TEXT NOT NULL, " +
                "$KEY_REQUESTED_ON DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE ($KEY_ACTIVITY) ON CONFLICT REPLACE)"

        val createTablePremiumRequest = "CREATE TABLE IF NOT EXISTS $TABLE_PREMIUM_REQUEST (" +
                "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "$KEY_ORDER_ID TEXT NOT NULL, " +
                "$KEY_PRODUCT_ID TEXT NOT NULL, " +
                "$KEY_NAME TEXT NOT NULL, " +
                "$KEY_ACTIVITY TEXT NOT NULL, " +
                "$KEY_REQUESTED_ON DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE ($KEY_ACTIVITY) ON CONFLICT REPLACE)"

        val createTableWallpaper = "CREATE TABLE IF NOT EXISTS $TABLE_WALLPAPERS (" +
                "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "$KEY_NAME TEXT NOT NULL, " +
                "$KEY_AUTHOR TEXT NOT NULL, " +
                "$KEY_URL TEXT NOT NULL, " +
                "$KEY_THUMB_URL TEXT NOT NULL, " +
                "$KEY_MIME_TYPE TEXT, " +
                "$KEY_SIZE INTEGER DEFAULT 0, " +
                "$KEY_COLOR INTEGER DEFAULT 0, " +
                "$KEY_WIDTH INTEGER DEFAULT 0, " +
                "$KEY_HEIGHT INTEGER DEFAULT 0, " +
                "UNIQUE ($KEY_URL))"

        val createTableBookmarkedIcons = "CREATE TABLE IF NOT EXISTS $TABLE_BOOKMARKED_ICONS (" +
                "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "$KEY_NAME TEXT NOT NULL, " +
                "$KEY_TITLE TEXT NOT NULL, " +
                "UNIQUE ($KEY_NAME) ON CONFLICT IGNORE)"

        db.execSQL(createTableRequest)
        db.execSQL(createTablePremiumRequest)
        db.execSQL(createTableWallpaper)
        db.execSQL(createTableBookmarkedIcons)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Need to clear shared preferences with version 3.4.0
        if (newVersion == 9) {
            Preferences.get(context).clearPreferences()
        }
        resetDatabase(db, oldVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        resetDatabase(db, oldVersion)
    }

    private fun resetDatabase(db: SQLiteDatabase, oldVersion: Int) {
        val tables = ArrayList<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    tables.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
        }

        val requests = getRequestedApps(db)
        val premiumRequests = getPremiumRequest(db)
        val wallpapers = getWallpapers(db)

        for (table in tables) {
            try {
                if (!table.equals("SQLITE_SEQUENCE", ignoreCase = true)) {
                    db.execSQL("DROP TABLE IF EXISTS $table")
                }
            } catch (ignored: Exception) {
            }
        }
        onCreate(db)

        for (request in requests) {
            addRequest(db, request)
        }

        addWallpapers(db, wallpapers)

        if (oldVersion <= 3) {
            return
        }

        for (premium in premiumRequests) {
            val r = Request.Builder()
                .name(premium.name)
                .activity(premium.activity)
                .orderId(premium.orderId)
                .productId(premium.productId)
                .requestedOn(premium.requestedOn)
                .build()
            addPremiumRequest(db, r)
        }
    }

    fun openDatabase(): Boolean {
        try {
            if (mDatabase == null || mDatabase?.get() == null) {
                LogUtil.e("Database error: openDatabase() database instance is null")
                return false
            }

            if (mDatabase?.get()?.sqliteDatabase == null) {
                mDatabase?.get()?.sqliteDatabase = mDatabase?.get()?.writableDatabase
            }

            if (mDatabase?.get()?.sqliteDatabase?.isOpen == false) {
                LogUtil.e("Database error: database openable false, trying to open the database again")
                mDatabase?.get()?.sqliteDatabase = mDatabase?.get()?.writableDatabase
            }
            return mDatabase?.get()?.sqliteDatabase?.isOpen == true
        } catch (e: Exception) {
            // Catch SQLiteException and NullPointerException
            LogUtil.e(Log.getStackTraceString(e))
            return false
        }
    }

    fun closeDatabase() {
        try {
            if (mDatabase == null || mDatabase?.get() == null) {
                LogUtil.e("Database error: closeDatabase() database instance is null")
                return
            }

            if (mDatabase?.get()?.sqliteDatabase == null) {
                LogUtil.e("Database error: trying to close database which is not opened")
                return
            }
            mDatabase?.get()?.sqliteDatabase?.close()
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
    }

    fun addRequest(db: SQLiteDatabase?, request: Request) {
        var database = db
        if (database == null) {
            if (!openDatabase()) {
                LogUtil.e("Database error: addRequest() failed to open database")
                return
            }
            database = mDatabase?.get()?.sqliteDatabase
        }

        val values = ContentValues()
        values.put(KEY_NAME, request.name)
        values.put(KEY_ACTIVITY, request.activity)

        var requestedOn = request.requestedOn
        if (requestedOn == null) requestedOn = TimeHelper.getLongDateTime()
        values.put(KEY_REQUESTED_ON, requestedOn)

        database?.insert(TABLE_REQUEST, null, values)
    }

    fun isRequested(activity: String): Boolean {
        if (!openDatabase()) {
            LogUtil.e("Database error: isRequested() failed to open database")
            return false
        }

        return try {
            mDatabase?.get()?.sqliteDatabase?.query(
                TABLE_REQUEST, null, "$KEY_ACTIVITY = ?",
                arrayOf(activity), null, null, null, null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getRequestedApps(db: SQLiteDatabase?): List<Request> {
        var database = db
        if (database == null) {
            if (!openDatabase()) {
                LogUtil.e("Database error: getRequestedApps() failed to open database")
                return ArrayList()
            }
            database = mDatabase?.get()?.sqliteDatabase
        }

        val requests = ArrayList<Request>()
        try {
            database?.query(TABLE_REQUEST, null, null, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val request = Request.Builder()
                            .name(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)))
                            .activity(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACTIVITY)))
                            .requestedOn(cursor.getString(cursor.getColumnIndexOrThrow(KEY_REQUESTED_ON)))
                            .requested(true)
                            .build()
                        requests.add(request)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return requests
    }

    fun addPremiumRequest(db: SQLiteDatabase?, request: Request) {
        var database = db
        if (database == null) {
            if (!openDatabase()) {
                LogUtil.e("Database error: addPremiumRequest() failed to open database")
                return
            }
            database = mDatabase?.get()?.sqliteDatabase
        }

        val values = ContentValues()
        values.put(KEY_ORDER_ID, request.orderId)
        values.put(KEY_PRODUCT_ID, request.productId)
        values.put(KEY_NAME, request.name)
        values.put(KEY_ACTIVITY, request.activity)

        var requestedOn = request.requestedOn
        if (requestedOn == null) requestedOn = TimeHelper.getLongDateTime()
        values.put(KEY_REQUESTED_ON, requestedOn)

        database?.insert(TABLE_PREMIUM_REQUEST, null, values)
    }

    fun getPremiumRequest(db: SQLiteDatabase?): List<Request> {
        var database = db
        if (database == null) {
            if (!openDatabase()) {
                LogUtil.e("Database error: getPremiumRequest() failed to open database")
                return ArrayList()
            }
            database = mDatabase?.get()?.sqliteDatabase
        }

        val requests = ArrayList<Request>()
        try {
            database?.query(TABLE_PREMIUM_REQUEST, null, null, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val request = Request.Builder()
                            .name(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)))
                            .activity(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ACTIVITY)))
                            .orderId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_ORDER_ID)))
                            .productId(cursor.getString(cursor.getColumnIndexOrThrow(KEY_PRODUCT_ID)))
                            .build()
                        requests.add(request)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return requests
    }

    fun addWallpapers(db: SQLiteDatabase?, list: List<*>?) {
        var database = db
        if (database == null) {
            if (!openDatabase()) {
                LogUtil.e("Database error: addWallpapers() failed to open database")
                return
            }
            database = mDatabase?.get()?.sqliteDatabase
        }

        if (list == null) return

        val query = "INSERT OR IGNORE INTO $TABLE_WALLPAPERS ($KEY_NAME,$KEY_AUTHOR,$KEY_URL,$KEY_THUMB_URL) VALUES (?,?,?,?);"
        val statement = database?.compileStatement(query)
        database?.beginTransaction()

        try {
            for (i in list.indices) {
                statement?.clearBindings()

                val wallpaper: Wallpaper?
                val item = list[i]
                if (item is Wallpaper) {
                    wallpaper = item
                } else {
                    wallpaper = JsonHelper.getWallpaper(item!!)
                }

                if (wallpaper != null) {
                    if (wallpaper.url != null) {
                        var name = wallpaper.name
                        if (name == null) name = ""

                        statement?.bindString(1, name)

                        if (wallpaper.author != null) {
                            statement?.bindString(2, wallpaper.author)
                        } else {
                            statement?.bindNull(2)
                        }

                        statement?.bindString(3, wallpaper.url)
                        statement?.bindString(4, wallpaper.thumbUrl)
                        statement?.execute()
                    }
                }
            }
            database?.setTransactionSuccessful()
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        } finally {
            database?.endTransaction()
        }
    }

    fun updateWallpaper(wallpaper: Wallpaper?) {
        if (!openDatabase()) {
            LogUtil.e("Database error: updateWallpaper() failed to open database")
            return
        }

        if (wallpaper == null) return

        val values = ContentValues()
        if (wallpaper.size > 0) {
            values.put(KEY_SIZE, wallpaper.size)
        }

        if (wallpaper.mimeType != null) {
            values.put(KEY_MIME_TYPE, wallpaper.mimeType)
        }

        if (wallpaper.dimensions != null) {
            values.put(KEY_WIDTH, wallpaper.dimensions.width)
            values.put(KEY_HEIGHT, wallpaper.dimensions.height)
        }

        if (wallpaper.color != 0) {
            values.put(KEY_COLOR, wallpaper.color)
        }

        if (values.size() > 0) {
            mDatabase?.get()?.sqliteDatabase?.update(
                TABLE_WALLPAPERS,
                values, "$KEY_URL = ?", arrayOf(wallpaper.url)
            )
        }
    }

    val wallpapersCount: Int
        get() {
            if (!openDatabase()) {
                LogUtil.e("Database error: getWallpapersCount() failed to open database")
                return 0
            }

            return try {
                mDatabase?.get()?.sqliteDatabase?.query(
                    TABLE_WALLPAPERS,
                    null, null, null, null, null, null, null
                )?.use { cursor ->
                    cursor.count
                } ?: 0
            } catch (e: Exception) {
                0
            }
        }

    fun getWallpaper(url: String): Wallpaper? {
        if (!openDatabase()) {
            LogUtil.e("Database error: getWallpaper() failed to open database")
            return null
        }

        var wallpaper: Wallpaper? = null
        try {
            mDatabase?.get()?.sqliteDatabase?.query(
                TABLE_WALLPAPERS,
                null, "$KEY_URL = ?", arrayOf(url), null, null, null, "1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val width = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WIDTH))
                        val height = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HEIGHT))
                        var dimensions: ImageSize? = null
                        if (width > 0 && height > 0) {
                            dimensions = ImageSize(width, height)
                        }

                        val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                        var name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME))
                        if (name.isEmpty()) {
                            name = "Wallpaper $id"
                        }

                        wallpaper = Wallpaper.Builder()
                            .name(name)
                            .author(cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTHOR)))
                            .url(cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL)))
                            .thumbUrl(cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMB_URL)))
                            .dimensions(dimensions)
                            .mimeType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_MIME_TYPE)))
                            .size(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SIZE)))
                            .color(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COLOR)))
                            .build()
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return wallpaper
    }

    fun getWallpapers(db: SQLiteDatabase?): MutableList<Wallpaper> {
        var database = db
        if (database == null) {
            if (!openDatabase()) {
                LogUtil.e("Database error: getWallpapers() failed to open database")
                return ArrayList()
            }
            database = mDatabase?.get()?.sqliteDatabase
        }

        val wallpapers = ArrayList<Wallpaper>()
        try {
            database?.query(
                TABLE_WALLPAPERS,
                null, null, null, null, null, "$KEY_ID ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val width = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WIDTH))
                        val height = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HEIGHT))
                        var dimensions: ImageSize? = null
                        if (width > 0 && height > 0) {
                            dimensions = ImageSize(width, height)
                        }

                        val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                        var name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME))
                        if (name.isEmpty()) {
                            name = "Wallpaper $id"
                        }

                        val wallpaper = Wallpaper.Builder()
                            .name(name)
                            .author(cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTHOR)))
                            .url(cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL)))
                            .thumbUrl(cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMB_URL)))
                            .color(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COLOR)))
                            .mimeType(cursor.getString(cursor.getColumnIndexOrThrow(KEY_MIME_TYPE)))
                            .dimensions(dimensions)
                            .size(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SIZE)))
                            .build()
                        wallpapers.add(wallpaper)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return wallpapers
    }

    val randomWallpaper: Wallpaper?
        get() {
            if (!openDatabase()) {
                LogUtil.e("Database error: getRandomWallpaper() failed to open database")
                return null
            }

            var wallpaper: Wallpaper? = null
            try {
                mDatabase?.get()?.sqliteDatabase?.query(
                    TABLE_WALLPAPERS,
                    null, null, null, null, null, "RANDOM()", "1"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                            var name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME))
                            if (name.isEmpty()) {
                                name = "Wallpaper $id"
                            }

                            wallpaper = Wallpaper.Builder()
                                .name(name)
                                .author(cursor.getString(cursor.getColumnIndexOrThrow(KEY_AUTHOR)))
                                .url(cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL)))
                                .thumbUrl(cursor.getString(cursor.getColumnIndexOrThrow(KEY_THUMB_URL)))
                                .build()
                        } while (cursor.moveToNext())
                    }
                }
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
            }
            return wallpaper
        }

    fun addBookmarkedIcon(drawableName: String, title: String) {
        if (!openDatabase()) {
            LogUtil.e("Database error: addBookmarkedIcon() failed to open database")
            return
        }

        val values = ContentValues()
        values.put(KEY_NAME, drawableName)
        values.put(KEY_TITLE, title)

        mDatabase?.get()?.sqliteDatabase?.insert(TABLE_BOOKMARKED_ICONS, null, values)
    }

    fun deleteBookmarkedIcon(drawableName: String) {
        if (!openDatabase()) {
            LogUtil.e("Database error: deleteBookmarkedIcon() failed to open database")
            return
        }

        mDatabase?.get()?.sqliteDatabase
            ?.delete(TABLE_BOOKMARKED_ICONS, "$KEY_NAME = ?", arrayOf(drawableName))
    }

    fun deleteBookmarkedIcons(drawableNames: List<String>) {
        if (!openDatabase()) {
            LogUtil.e("Database error: deleteBookmarkedIcons() failed to open database")
            return
        }

        val inPart = "\"" + TextUtils.join("\", \"", drawableNames) + "\""

        mDatabase?.get()?.sqliteDatabase?.execSQL(
            "DELETE FROM $TABLE_BOOKMARKED_ICONS WHERE $KEY_NAME IN ($inPart)"
        )
    }

    fun isIconBookmarked(drawableName: String): Boolean {
        if (!openDatabase()) {
            LogUtil.e("Database error: isIconBookmarked() failed to open database")
            return false
        }

        return try {
            mDatabase?.get()?.sqliteDatabase?.query(
                TABLE_BOOKMARKED_ICONS, null, "$KEY_NAME = ?",
                arrayOf(drawableName), null, null, null, null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getBookmarkedIcons(context: Context?): List<Icon> {
        if (!openDatabase()) {
            LogUtil.e("Database error: getBookmarkedIcons() failed to open database")
            return ArrayList()
        }

        val icons = ArrayList<Icon>()
        try {
            mDatabase?.get()?.sqliteDatabase?.query(
                TABLE_BOOKMARKED_ICONS,
                null, null, null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val drawableName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME))
                        val title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE))
                        val resId = DrawableHelper.getDrawableId(drawableName)
                        icons.add(Icon(drawableName, null, resId).apply { this.title = title })
                    } while (cursor.moveToNext())
                }
            }
            Collections.sort(icons, Icon.TitleComparator)
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return icons
    }

    fun deleteIconRequestData() {
        if (!openDatabase()) {
            LogUtil.e("Database error: deleteIconRequestData() failed to open database")
            return
        }

        mDatabase?.get()?.sqliteDatabase?.delete("SQLITE_SEQUENCE", "NAME = ?", arrayOf(TABLE_REQUEST))
        mDatabase?.get()?.sqliteDatabase?.delete(TABLE_REQUEST, null, null)
    }

    fun deleteWallpapers() {
        if (!openDatabase()) {
            LogUtil.e("Database error: deleteWallpapers() failed to open database")
            return
        }

        mDatabase?.get()?.sqliteDatabase?.delete("SQLITE_SEQUENCE", "NAME = ?", arrayOf(TABLE_WALLPAPERS))
        mDatabase?.get()?.sqliteDatabase?.delete(TABLE_WALLPAPERS, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "candybar_database"
        private const val DATABASE_VERSION = 11

        private const val TABLE_REQUEST = "icon_request"
        private const val TABLE_PREMIUM_REQUEST = "premium_request"
        private const val TABLE_WALLPAPERS = "wallpapers"
        private const val TABLE_BOOKMARKED_ICONS = "bookmarked_icons"

        private const val KEY_ID = "id"

        private const val KEY_ORDER_ID = "order_id"
        private const val KEY_PRODUCT_ID = "product_id"

        private const val KEY_NAME = "name"
        private const val KEY_ACTIVITY = "activity"
        private const val KEY_REQUESTED_ON = "requested_on"

        private const val KEY_AUTHOR = "author"
        private const val KEY_THUMB_URL = "thumbUrl"
        private const val KEY_URL = "url"
        private const val KEY_MIME_TYPE = "mimeType"
        private const val KEY_COLOR = "color"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_SIZE = "size"

        private const val KEY_TITLE = "title"

        private var mDatabase: WeakReference<Database>? = null

        @JvmStatic
        fun get(context: Context): Database {
            if (mDatabase == null || mDatabase?.get() == null) {
                mDatabase = WeakReference(Database(context))
            }
            return mDatabase!!.get()!!
        }
    }
}
