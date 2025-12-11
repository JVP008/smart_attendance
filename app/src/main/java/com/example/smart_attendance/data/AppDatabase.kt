package com.example.smart_attendance.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*


@Entity(tableName = "offline_attendance")
data class OfflineAttendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val name: String,
    val enrollmentNumber: String,
    val status: String
)

@Dao
interface OfflineAttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: OfflineAttendance)

    @Query("SELECT * FROM offline_attendance")
    fun getAllAttendance(): LiveData<List<OfflineAttendance>>

    @Query("DELETE FROM offline_attendance")
    suspend fun clearAttendance()
}

@Database(entities = [Student::class, Attendance::class, OfflineAttendance::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun offlineAttendanceDao(): OfflineAttendanceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_attendance_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
