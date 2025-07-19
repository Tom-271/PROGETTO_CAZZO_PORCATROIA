package com.example.progetto_tosa.data

import android.content.Context
import androidx.lifecycle.*
import androidx.room.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/* ================== ENTITY ================== */
@Entity(
    tableName = "body_fat_entries",
    indices = [Index(value = ["userId", "epochDay"], unique = true)]
)
data class BodyFatEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,          // UID Firebase
    val epochDay: Long,          // LocalDate.toEpochDay()
    val bodyFatPercent: Float
)

/* ================== DAO ================== */
@Dao
interface BodyFatDao {

    @Query("SELECT * FROM body_fat_entries WHERE userId = :userId ORDER BY epochDay ASC")
    suspend fun getAll(userId: String): List<BodyFatEntry>

    @Upsert
    suspend fun upsert(entry: BodyFatEntry)

    @Query("SELECT * FROM body_fat_entries WHERE userId = :userId AND epochDay = :epochDay LIMIT 1")
    suspend fun getByDay(userId: String, epochDay: Long): BodyFatEntry?

    @Query("DELETE FROM body_fat_entries WHERE userId = :userId")
    suspend fun clearUser(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BodyFatEntry>)
}

/* ================== DB ================== */
@Database(entities = [BodyFatEntry::class], version = 3, exportSchema = true)
abstract class BodyFatDb : RoomDatabase() {
    abstract fun dao(): BodyFatDao

    companion object {
        @Volatile private var INSTANCE: BodyFatDb? = null
        fun get(context: Context): BodyFatDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BodyFatDb::class.java,
                    "body_fat_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

/* ================== REPOSITORY ================== */
class BodyFatRepository(
    private val dao: BodyFatDao,
    private val userId: String
) {
    suspend fun all() = dao.getAll(userId)

    suspend fun addOrReplace(percent: Float, date: LocalDate) =
        dao.upsert(
            BodyFatEntry(
                userId = userId,
                epochDay = date.toEpochDay(),
                bodyFatPercent = percent
            )
        )

    suspend fun get(date: LocalDate) = dao.getByDay(userId, date.toEpochDay())

    suspend fun replaceAll(entries: List<BodyFatEntry>) {
        dao.clearUser(userId)
        if (entries.isNotEmpty()) {
            val normalized = entries.map { if (it.userId == userId) it else it.copy(userId = userId) }
            dao.insertAll(normalized)
        }
    }
}

/* ================== VIEWMODEL ================== */
class BodyFatViewModel(private val repo: BodyFatRepository) : ViewModel() {

    private val _entries = MutableLiveData<List<BodyFatEntry>>(emptyList())
    val entries: LiveData<List<BodyFatEntry>> = _entries

    fun load() {
        viewModelScope.launch { _entries.postValue(repo.all()) }
    }

    fun addMeasurement(percent: Float, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repo.addOrReplace(percent, date)
            _entries.postValue(repo.all())
        }
    }

    fun getMeasurement(date: LocalDate, callback: (BodyFatEntry?) -> Unit) {
        viewModelScope.launch { callback(repo.get(date)) }
    }

    fun replaceAll(list: List<BodyFatEntry>) {
        viewModelScope.launch {
            repo.replaceAll(list)
            _entries.postValue(repo.all())
        }
    }
}

/* ================== FACTORY ================== */
class BodyFatVmFactory(
    private val context: Context,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = BodyFatDb.get(context)
        val repo = BodyFatRepository(db.dao(), userId)
        return BodyFatViewModel(repo) as T
    }
}
