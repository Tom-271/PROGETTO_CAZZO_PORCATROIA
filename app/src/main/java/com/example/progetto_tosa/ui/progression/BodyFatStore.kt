package com.example.progetto_tosa.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate

@Entity(
    tableName = "body_fat_entries",
    indices = [Index(value = ["userId", "epochDay"], unique = true)]
)
data class BodyFatEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val epochDay: Long,
    val bodyFatPercent: Float?,
    val bodyWeightKg: Float? = null,
    val leanMassKg: Float? = null
)

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

@Database(entities = [BodyFatEntry::class], version = 5, exportSchema = true)
abstract class BodyFatDb : RoomDatabase() {
    abstract fun dao(): BodyFatDao

    companion object {
        @Volatile private var INSTANCE: BodyFatDb? = null

        // Migration from version 4 to 5: add nullable columns
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE body_fat_entries ADD COLUMN bodyFatPercent REAL")
                database.execSQL("ALTER TABLE body_fat_entries ADD COLUMN bodyWeightKg REAL")
                database.execSQL("ALTER TABLE body_fat_entries ADD COLUMN leanMassKg REAL")
            }
        }

        fun get(context: Context): BodyFatDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BodyFatDb::class.java,
                    "body_fat_db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

class BodyFatRepository(private val dao: BodyFatDao, private val userId: String) {
    suspend fun all() = dao.getAll(userId)

    /** Aggiunge o aggiorna body fat% e peso */
    suspend fun addOrReplace(percent: Float, weight: Float?, date: LocalDate) {
        dao.upsert(
            BodyFatEntry(
                userId = userId,
                epochDay = date.toEpochDay(),
                bodyFatPercent = percent,
                bodyWeightKg = weight
            )
        )
    }

    /** Aggiunge o aggiorna solo la lean mass per la data indicata */
    suspend fun addOrReplaceLean(lean: Float, date: LocalDate) {
        val epoch = date.toEpochDay()
        val existing = dao.getByDay(userId, epoch)
        val bf   = existing?.bodyFatPercent ?: 0f
        val wt   = existing?.bodyWeightKg
        dao.upsert(
            BodyFatEntry(
                userId = userId,
                epochDay = epoch,
                bodyFatPercent = bf,
                bodyWeightKg = wt,
                leanMassKg = lean
            )
        )
    }

    suspend fun get(date: LocalDate) = dao.getByDay(userId, date.toEpochDay())

    suspend fun replaceAll(entries: List<BodyFatEntry>) {
        dao.clearUser(userId)
        if (entries.isNotEmpty()) {
            val normalized = entries.map {
                if (it.userId == userId) it else it.copy(userId = userId)
            }
            dao.insertAll(normalized)
        }
    }
}

class BodyFatViewModel(private val repo: BodyFatRepository) : ViewModel() {
    private val _entries = androidx.lifecycle.MutableLiveData<List<BodyFatEntry>>(emptyList())
    val entries: androidx.lifecycle.LiveData<List<BodyFatEntry>> = _entries

    fun load() {
        viewModelScope.launch {
            _entries.postValue(repo.all())
        }
    }

    fun addMeasurement(percent: Float, weight: Float?, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repo.addOrReplace(percent, weight, date)
            _entries.postValue(repo.all())
        }
    }

    /** Aggiunge o aggiorna la lean mass e ricarica i dati */
    fun addLeanMass(lean: Float, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repo.addOrReplaceLean(lean, date)
            _entries.postValue(repo.all())
        }
    }

    fun getMeasurement(date: LocalDate, callback: (BodyFatEntry?) -> Unit) {
        viewModelScope.launch {
            callback(repo.get(date))
        }
    }

    fun replaceAll(list: List<BodyFatEntry>) {
        viewModelScope.launch {
            repo.replaceAll(list)
            _entries.postValue(repo.all())
        }
    }
}

class BodyFatVmFactory(private val context: Context, private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = BodyFatDb.get(context)
        val repo = BodyFatRepository(db.dao(), userId)
        return BodyFatViewModel(repo) as T
    }
}
