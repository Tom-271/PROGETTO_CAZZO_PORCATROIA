package com.example.progetto_tosa.ui.account

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.ActivityRegisterBinding
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val vm: RegisterViewModel by viewModels()

    // Tieni un parser locale anche qui
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        binding.vm = vm
        binding.lifecycleOwner = this

        // Quando ViewModel richiede di mostrare il DatePicker
        vm.showDatePickerEvent.observe(this) {
            showDatePicker()
        }

        // Mostra eventuali errori
        vm.errorMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                vm.errorMessageHandled()
            }
        }

        // Alla registrazione riuscita, chiude activity
        vm.registrationSuccessEvent.observe(this) {
            Toast.makeText(this, "Registrazione avvenuta con successo!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDatePicker() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, yy, mm, dd ->
            calendar.set(yy, mm, dd)
            // formatto con il dateFormat locale
            val formatted = dateFormat.format(calendar.time)
            vm.birthDate.value = formatted
        }, y, m, d).show()
    }
}
