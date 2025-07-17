package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class AccountFragment : Fragment() {

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var binding: FragmentAccountBinding

    // Registrazione per il risultato dello scanner QR (sostituisce IntentIntegrator)
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            viewModel.linkAthleteToPT(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_account,
            container,
            false
        )
        binding.apply {
            vm = viewModel
            navController = findNavController()
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mostra il QR per l'atleta
        binding.buttonQr.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                QrDialogFragment.newInstance(uid)
                    .show(childFragmentManager, "qr_dialog")
            }
        }

        // Avvia scanner QR con la nuova API
        binding.buttonCamera.setOnClickListener {
            launchQrScanner()
        }
    }

    private fun launchQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Inquadra il QR dell'atleta")
            setBeepEnabled(true)
            setOrientationLocked(false)
            setCameraId(0)  // Usa la fotocamera posteriore
        }
        qrScannerLauncher.launch(options)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateUI()
        updateRoleButtons()
    }

    private fun updateRoleButtons() {
        val loggedIn = viewModel.isLoggedIn.get()
        val trainer = viewModel.isTrainer.get()

        binding.apply {
            buttonQr.visibility = if (loggedIn && !trainer) View.VISIBLE else View.GONE
            buttonCamera.visibility = if (loggedIn && trainer) View.VISIBLE else View.GONE
        }
    }
}