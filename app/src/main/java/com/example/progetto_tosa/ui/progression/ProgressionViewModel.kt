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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    val bodyFatEntries: LiveData<List<BodyFatEntry>> = bodyFatVm.entries

    private val _goals = MutableLiveData<GoalsUi>()
    val goals: LiveData<GoalsUi> = _goals

    fun loadBodyFat() = bodyFatVm.load()

    fun loadGoals() {
        val user = auth.currentUser ?: return
        val ptRef = firestore.collection("personal_trainers").document(user.uid)
        ptRef.get().addOnSuccessListener { ptSnap ->
            val isPt = ptSnap.getBoolean("isPersonalTrainer") == true
            if (isPt) {
                _goals.value = GoalsUi(
                    targetLean = ptSnap.getDouble("targetLeanMass"),
                    targetFat = ptSnap.getDouble("targetFatMass")
                )
            } else {
                firestore.collection("users").document(user.uid).get()
                    .addOnSuccessListener { snap ->
                        _goals.value = GoalsUi(
                            targetLean = snap.getDouble("targetLeanMass"),
                            targetFat = snap.getDouble("targetFatMass"),
                            currentBodyFat = snap.getDouble("currentBodyFatPercent"),
                            currentBodyWeight = snap.getDouble("currentBodyWeightKg")
                        )
                    }
            }
        }
    }

    fun addBodyFat(percent: Float, weight: Float?, date: LocalDate = LocalDate.now()) {
        bodyFatVm.addMeasurement(percent, weight, date)
        auth.currentUser?.let { u ->
            val epoch = date.toEpochDay()
            val entry = mutableMapOf<String, Any>(
                "epochDay" to epoch,
                "bodyFatPercent" to percent,
                "updatedAt" to Timestamp.now()
            )
            weight?.let { entry["bodyWeightKg"] = it }

            firestore.collection("users")
                .document(u.uid)
                .collection("bodyFatEntries")
                .document(epoch.toString())
                .set(entry)

            firestore.collection("users").document(u.uid)
                .set(mapOf(
                    "currentBodyFatPercent" to percent,
                    "lastBodyFatEpochDay" to epoch,
                    "currentBodyWeightKg" to (weight ?: _goals.value?.currentBodyWeight)
                ), SetOptions.merge())
        }
    }

    fun replaceAllFromCloud(list: List<BodyFatEntry>) =
        bodyFatVm.replaceAll(list)

    fun getMeasurement(date: LocalDate, cb: (BodyFatEntry?) -> Unit) =
        bodyFatVm.getMeasurement(date, cb)

    fun updateGoals(newLean: Double? = null, newFat: Double? = null) {
        val user = auth.currentUser ?: return
        val ptRef = firestore.collection("personal_trainers").document(user.uid)
        ptRef.get().addOnSuccessListener { ptSnap ->
            val isPt = ptSnap.getBoolean("isPersonalTrainer") == true
            val targetRef = if (isPt) ptRef
            else firestore.collection("users").document(user.uid)
            val data = mutableMapOf<String, Any>()
            newLean?.let { data["targetLeanMass"] = it }
            newFat?.let { data["targetFatMass"] = it }
            if (data.isNotEmpty()) {
                targetRef.set(data, SetOptions.merge())
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