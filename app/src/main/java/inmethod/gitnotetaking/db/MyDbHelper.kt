package inmethod.gitnotetaking.db


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper


class MyDbHelper(context: Context, name: String,
                 factory: CursorFactory?, version: Int)
    : SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(RemoteGitDAO.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase,
                           oldVersion: Int,
                           newVersion: Int) {

        if (oldVersion == 1) {
            db.execSQL("DROP TABLE IF EXISTS " + RemoteGitDAO.TABLE_NAME)
            onCreate(db)
            return
        }

        if (oldVersion < 3) {
            try {
                db.execSQL(
                    "ALTER TABLE " + RemoteGitDAO.TABLE_NAME +
                            " ADD COLUMN " + RemoteGitDAO.AUTH_TYPE_COLUMN +
                            " TEXT NOT NULL DEFAULT '" + RemoteGit.AUTH_TYPE_PASSWORD + "'"
                )
            } catch (ignored: Exception) {
                // Column may already exist
            }
            try {
                db.execSQL(
                    "UPDATE " + RemoteGitDAO.TABLE_NAME +
                            " SET " + RemoteGitDAO.AUTH_TYPE_COLUMN + " = '" + RemoteGit.AUTH_TYPE_WEB_OAUTH + "'" +
                            " WHERE LOWER(" + RemoteGitDAO.URL_COLUMN + ") LIKE '%github.com%'" +
                            " AND " + RemoteGitDAO.PWD_COLUMN + " != ''" +
                            " AND " + RemoteGitDAO.PWD_COLUMN + " NOT LIKE 'ghp_%'" +
                            " AND " + RemoteGitDAO.PWD_COLUMN + " NOT LIKE 'github_pat_%'"
                )
            } catch (ignored: Exception) {
            }
        }
    }

    companion object {

        // 資料庫名稱
        val DATABASE_NAME = "gitnotetaking.db"
        // 資料庫版本，資料結構改變的時候要更改這個數字，通常是加一
        val VERSION = 3

        // 需要資料庫的元件呼叫這個函式，這個函式在一般的應用都不需要修改
        fun getDatabase(context: Context): SQLiteDatabase {
            return MyDbHelper(context, DATABASE_NAME, null, VERSION).writableDatabase
        }

    }

}
