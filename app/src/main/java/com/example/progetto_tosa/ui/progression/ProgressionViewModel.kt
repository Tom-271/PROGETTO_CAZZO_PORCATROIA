package com.example.progetto_tosa.ui.progression

import androidx.lifecycle.*
import com.example.progetto_tosa.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalsUi(
    val targetLean: Double? = null,
    val targetFat: Double? = null,
    val currentBodyFat: Double? = null,
    val currentBodyWeight: Double? = null
)

class ProgressionViewModel(
    private val bodyFatVm: BodyFatViewModel,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    val bodyFatEntries = bodyFatVm.entries

    private val _goals = MutableLiveData<GoalsUi>()
    val goals: LiveData<GoalsUi> = _goals

    fun loadBodyFat() = bodyFatVm.load()

    fun loadGoals() {
        val user = auth.currentUser ?: return
        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { snap ->
                _goals.value = GoalsUi(
                    targetLean = snap.getDouble("targetLeanMass"),
                    targetFat = snap.getDouble("targetFatMass"),
                    currentBodyFat = snap.getDouble("currentBodyFatPercent"),
                    currentBodyWeight = snap.getDouble("currentBodyWeightKg")
                )
            }
    }

    fun addBodyFat(percent: Float, weight: Float?, date: LocalDate = LocalDate.now()) {
        bodyFatVm.addMeasurement(percent, weight, date)
        auth.currentUser?.let { u ->
            val epoch = date.toEpochDay()
            val data = mutableMapOf<String, Any>(
                "epochDay" to epoch,
                "bodyFatPercent" to percent,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            if (weight != null) data["bodyWeightKg"] = weight
            firestore.collection("users").document(u.uid)
                .collection("bodyFatEntries").document(epoch.toString())
                .set(data)
            firestore.collection("users").document(u.uid)
                .set(
                    mapOf(
                        "currentBodyFatPercent" to percent,
                        "lastBodyFatEpochDay" to epoch,
                        "currentBodyWeightKg" to (weight ?: _goals.value?.currentBodyWeight)
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        }
    }

    fun replaceAllFromCloud(list: List<BodyFatEntry>) { bodyFatVm.replaceAll(list) }

    fun getMeasurement(date: LocalDate, cb: (BodyFatEntry?) -> Unit) = bodyFatVm.getMeasurement(date, cb)
}

class ProgressionVmFactory(
    private val context: android.content.Context,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(cls: Class<T>): T {
        val db = BodyFatDb.get(context)
        val repo = BodyFatRepository(db.dao(), userId)
        val bodyFatVm = BodyFatViewModel(repo)
        return ProgressionViewModel(
            bodyFatVm,
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        ) as T
    }
}