package com.example.progetto_tosa.ui.progression

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_tosa.data.BodyFatDb
import com.example.progetto_tosa.data.BodyFatEntry
import com.example.progetto_tosa.data.BodyFatRepository
import com.example.progetto_tosa.data.BodyFatViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

/** UI model for goals and current metrics */
data class GoalsUi(
    val targetWeight: Double? = null,
    val targetLean: Double?   = null,
    val targetFat: Double?    = null,
    val currentBodyFat: Double?    = null,
    val currentBodyWeight: Double? = null,
    val currentLeanMass: Double?   = null
)

class ProgressionViewModel(
    private val bodyFatVm: BodyFatViewModel,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    val bodyFatEntries: LiveData<List<BodyFatEntry>> = bodyFatVm.entries
    private val _goals = MutableLiveData<GoalsUi>()
    val goals: LiveData<GoalsUi> = _goals

    fun loadBodyFat() = bodyFatVm.load()

    /** Helper: restituisce il DocumentReference di base (users/{uid} oppure personal_trainers/{uid}) */
    private fun withBaseDocRef(onReady: (DocumentReference) -> Unit) {
        val user = auth.currentUser ?: return
        val ptRef = firestore.collection("personal_trainers").document(user.uid)
        ptRef.get()
            .addOnSuccessListener { snap ->
                val root = if (snap.exists() || snap.getBoolean("isPersonalTrainer") == true)
                    "personal_trainers"
                else
                    "users"
                onReady(firestore.collection(root).document(user.uid))
            }
            .addOnFailureListener {
                // fallback sicuro su users
                onReady(firestore.collection("users").document(user.uid))
            }
    }

    fun loadGoals() {
        auth.currentUser?.let { user ->
            val ptRef = firestore.collection("personal_trainers").document(user.uid)
            ptRef.get().addOnSuccessListener { snap ->
                if (snap.getBoolean("isPersonalTrainer") == true || snap.exists()) {
                    _goals.value = GoalsUi(
                        targetWeight = snap.getDouble("targetWeight"),
                        targetLean   = snap.getDouble("targetLeanMass"),
                        targetFat    = snap.getDouble("targetFatMass")
                    )
                } else {
                    firestore.collection("users").document(user.uid)
                        .get().addOnSuccessListener { uSnap ->
                            _goals.value = GoalsUi(
                                targetWeight      = uSnap.getDouble("targetWeight"),
                                targetLean        = uSnap.getDouble("targetLeanMass"),
                                targetFat         = uSnap.getDouble("targetFatMass"),
                                currentBodyFat    = uSnap.getDouble("currentBodyFatPercent"),
                                currentBodyWeight = uSnap.getDouble("currentBodyWeightKg"),
                                currentLeanMass   = uSnap.getDouble("currentLeanMassKg")
                            )
                        }
                }
            }
        }
    }

    fun addBodyFat(percent: Float, weight: Float?, date: LocalDate = LocalDate.now()) {
        // Salva sempre nel DB locale per i grafici
        bodyFatVm.addMeasurement(percent, weight, date)

        val epoch = date.toEpochDay()
        val entry = mutableMapOf<String, Any>(
            "epochDay" to epoch,
            "bodyFatPercent" to percent,
            "updatedAt" to Timestamp.now()
        )
        weight?.let { entry["bodyWeightKg"] = it }

        val summary = mutableMapOf<String, Any>(
            "currentBodyFatPercent" to percent,
            "lastBodyFatEpochDay"   to epoch
        )
        weight?.let { summary["currentBodyWeightKg"] = it }

        // Scrivi su Firestore nel ramo corretto (users/ oppure personal_trainers/)
        withBaseDocRef { baseRef ->
            baseRef.collection("bodyFatEntries").document(epoch.toString())
                .set(entry, SetOptions.merge())
            baseRef.set(summary, SetOptions.merge())
        }
    }

    fun addLeanMass(lean: Float, date: LocalDate = LocalDate.now()) {
        // Salva nel DB locale
        bodyFatVm.addLeanMass(lean, date)

        val epoch = date.toEpochDay()
        val entry = mapOf(
            "epochDay" to epoch,
            "leanMassKg" to lean,
            "updatedAt" to Timestamp.now()
        )
        val summary = mapOf(
            "currentLeanMassKg" to lean,
            "lastLeanEpochDay"  to epoch
        )

        // Scrivi su Firestore nel ramo corretto (users/ oppure personal_trainers/)
        withBaseDocRef { baseRef ->
            baseRef.collection("bodyFatEntries").document(epoch.toString())
                .set(entry, SetOptions.merge())
            baseRef.set(summary, SetOptions.merge())
        }
    }

    fun getMeasurement(date: LocalDate, cb: (BodyFatEntry?) -> Unit) =
        bodyFatVm.getMeasurement(date, cb)

    fun replaceAllFromCloud(list: List<BodyFatEntry>) =
        bodyFatVm.replaceAll(list)

    fun updateGoals(
        newWeight: Double? = null,
        newLean: Double?   = null,
        newFat: Double?    = null
    ) {
        auth.currentUser?.let { user ->
            val ptRef = firestore.collection("personal_trainers").document(user.uid)
            ptRef.get().addOnSuccessListener { snap ->
                val ref =
                    if (snap.getBoolean("isPersonalTrainer") == true || snap.exists())
                        ptRef
                    else
                        firestore.collection("users").document(user.uid)

                val data = mutableMapOf<String, Any>()
                newWeight?.let { data["targetWeight"]   = it }
                newLean?.let   { data["targetLeanMass"] = it }
                newFat?.let    { data["targetFatMass"]  = it }
                if (data.isNotEmpty()) ref.set(data, SetOptions.merge())
            }
        }
    }
}

class ProgressionVmFactory(
    private val context: Context,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db        = BodyFatDb.get(context)
        val repo      = BodyFatRepository(db.dao(), userId)
        val bodyFatVm = BodyFatViewModel(repo)
        return ProgressionViewModel(
            bodyFatVm,
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        ) as T
    }
}
