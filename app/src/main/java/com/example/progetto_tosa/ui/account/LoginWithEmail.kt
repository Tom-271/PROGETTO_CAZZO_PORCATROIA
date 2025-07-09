package com.example.progetto_tosa.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.ActivityLoginWithEmailBinding

class LoginWithEmail : AppCompatActivity() {

    private lateinit var binding: ActivityLoginWithEmailBinding
    private val vm: LoginWithEmailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login_with_email)
        binding.vm = vm
        binding.lifecycleOwner = this

        vm.navigateEvent.observe(this) { event ->
            event.getIfNotHandled()?.let { dest ->
                when (dest) {
                    is LoginWithEmailViewModel.Destination.SHOW_TOAST ->
                        Toast.makeText(this, dest.msg, Toast.LENGTH_LONG).show()
                    LoginWithEmailViewModel.Destination.LOGIN_SUCCESS -> animateSuccess()
                    LoginWithEmailViewModel.Destination.GO_REGISTER   ->
                        startActivity(Intent(this, RegisterActivity::class.java))
                    LoginWithEmailViewModel.Destination.GO_MAIN       -> {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            putExtra("navigateTo", "account")
                        })
                        finish()
                    }
                }
            }
        }
    }

    private fun animateSuccess() {
        val btn = binding.btnLogin
        btn.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
        btn.animate()
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(150)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                btn.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(100)
                    .withEndAction { vm.onRegisterComplete() }
                    .start()
            }
            .start()
    }
}
