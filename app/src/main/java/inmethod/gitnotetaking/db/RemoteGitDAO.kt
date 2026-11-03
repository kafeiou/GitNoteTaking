package inmethod.gitnotetaking.db


import android.content.ContentValues
import android.content.Context

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.util.*

// 資料功能類別
class RemoteGitDAO(context: Context) {

    companion object {
        // 表格名稱
        val TABLE_NAME = "REMOTE_GIT"

        // 編號表格欄位名稱，固定不變
        val KEY_ID = "_id"

        // 其它表格欄位名稱
        val REMOTE_NAME_COLUMN = "REMOTE_NAME"
        val URL_COLUMN = "URL"
        val UID_COLUMN = "UID"
        val PWD_COLUMN = "PWD"
        val NICKNAME_COLUMN = "NICKNAME"
        val STATUS_COLUMN = "STATUS"
        val BRANCH_COLUMN = "BRANCH"
        var AUTHOR_NAME = "AUTHOR_NAME"
        var AUTHOR_EMAIL = "AUTHOR_EMAIL"
        val AUTH_TYPE_COLUMN = "AUTH_TYPE"

        // 使用上面宣告的變數建立表格的SQL敘述
        val CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                REMOTE_NAME_COLUMN + " TEXT NOT NULL , " +
                URL_COLUMN + " TEXT NOT NULL , " +
                UID_COLUMN + " TEXT NOT NULL, " +
                PWD_COLUMN + " TEXT NOT NULL, " +
                NICKNAME_COLUMN + " TEXT NOT NULL, " +
                STATUS_COLUMN + " INTEGER DEFAULT 0," +
                BRANCH_COLUMN + " TEXT NOT NULL, " +
                AUTHOR_NAME + " TEXT NOT NULL, " +
                AUTHOR_EMAIL + " TEXT NOT NULL, " +
                AUTH_TYPE_COLUMN + " TEXT NOT NULL DEFAULT '" + RemoteGit.AUTH_TYPE_PASSWORD + "') "
    }

    // 資料庫物件
    private val db: SQLiteDatabase = MyDbHelper.getDatabase(context)

    // 讀取所有記事資料
    val all: ArrayList<RemoteGit>
        get() {
            val result = ArrayList<RemoteGit>()
            val cursor = db.query(
                    TABLE_NAME, null, null, null, null, null, null, null)

            while (cursor.moveToNext()) {
                result.add(getRecord(cursor))
            }

            cursor.close()
            return result
        }

    // 取得資料數量
    val count: Int
        get() {
            var result = 0
            val cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null)

            if (cursor.moveToNext()) {
                result = cursor.getInt(0)
            }
            cursor.close()
            return result
        }

    // 關閉資料庫，一般的應用都不需要修改
    fun close() {
        db.close()
    }

    // 新增參數指定的物件
    fun insert(item: RemoteGit): RemoteGit {
        val cv = ContentValues()
        itemToContentValues(item, cv)
        val id = db.insert(TABLE_NAME, null, cv)
        item.id = id
        return item
    }

    // 修改參數指定的物件
    fun update(item: RemoteGit): Boolean {
        val cv = ContentValues()
        itemToContentValues(item, cv)
        val where = KEY_ID + "=" + item.id
        return db.update(TABLE_NAME, cv, where, null) > 0
    }

    fun updateByRemoteUrl(item: RemoteGit): Boolean {
        val cv = ContentValues()
        itemToContentValues(item, cv)
        val where = URL_COLUMN + "='" + item.url + "'"
        return db.update(TABLE_NAME, cv, where, null) > 0
    }


    private fun itemToContentValues(item: RemoteGit, cv: ContentValues) {
        cv.put(REMOTE_NAME_COLUMN, item.remoteName)
        cv.put(URL_COLUMN, item.url)
        cv.put(UID_COLUMN, item.uid)
        cv.put(PWD_COLUMN, item.pwd)
        cv.put(NICKNAME_COLUMN, item.nickname)
        cv.put(STATUS_COLUMN, item.status)
        cv.put(BRANCH_COLUMN, item.branch)
        cv.put(AUTHOR_NAME, item.author_name)
        cv.put(AUTHOR_EMAIL, item.author_email)
        val authType = if (item.auth_type.isNotBlank()) item.auth_type else RemoteGit.inferAuthType(item.url, item.pwd)
        cv.put(AUTH_TYPE_COLUMN, authType)
    }

    // 刪除參數指定編號的資料
    fun delete(id: Long): Boolean {
        val where = KEY_ID + "=" + id
        return db.delete(TABLE_NAME, where, null) > 0
    }


    // 刪除參數指定編號的資料
    fun delete(sRemoteUrl: String): Boolean {
        val where = URL_COLUMN + "='" + sRemoteUrl + "'"
        return db.delete(TABLE_NAME, where, null) > 0
    }

    // 取得指定編號的資料物件
    operator fun get(id: Long): RemoteGit? {
        var item: RemoteGit? = null
        val where = KEY_ID + "=" + id
        val result = db.query(
                TABLE_NAME, null, where, null, null, null, null, null)

        if (result.moveToFirst()) {
            item = getRecord(result)
        }

        result.close()
        return item
    }

    fun getByURL(url: String): RemoteGit? {
        var item: RemoteGit? = null
        val where = URL_COLUMN + " = '" + url + "'"
        val result = db.query(
                TABLE_NAME, null, where, null, null, null, null, null)

        if (result.moveToFirst()) {
            item = getRecord(result)
        }

        result.close()
        return item
    }

    // 把Cursor目前的資料包裝為物件
    fun getRecord(cursor: Cursor): RemoteGit {
        val result = RemoteGit()
        result.id = cursor.getLong(0)
        result.remoteName = cursor.getString(1)
        result.url = cursor.getString(2)
        result.uid = cursor.getString(3)
        result.pwd = cursor.getString(4)
        result.nickname = cursor.getString(5)
        result.status = cursor.getLong(6)
        result.branch = cursor.getString(7)
        result.author_name = cursor.getString(8)
        result.author_email = cursor.getString(9)
        val authTypeIdx = cursor.getColumnIndex(AUTH_TYPE_COLUMN)
        if (authTypeIdx >= 0 && !cursor.isNull(authTypeIdx)) {
            result.auth_type = cursor.getString(authTypeIdx)
        } else {
            result.auth_type = RemoteGit.inferAuthType(result.url, result.pwd)
        }
        return result
    }


}
