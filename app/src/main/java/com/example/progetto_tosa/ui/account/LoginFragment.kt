package com.example.progetto_tosa.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Collega il layout XML
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Trova i pulsanti nel layout
        val btnMailLogin = view.findViewById<Button>(R.id.btnMailLogin)
        val btnGoogleLogin = view.findViewById<Button>(R.id.btnGoogleLogin)

        // Clic su "Accedi con Email"
        btnMailLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginWithEmail::class.java)
            startActivity(intent)
        }

        // Clic su "Accedi con Google"
        btnGoogleLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginWithGoogle::class.java)
            startActivity(intent)
        }
    }
}
