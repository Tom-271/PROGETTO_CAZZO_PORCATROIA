package com.example.progetto_tosa.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentQrDialogBinding

class QrDialogFragment : DialogFragment() {
    private var _binding: FragmentQrDialogBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_UID = "arg_uid"
        fun newInstance(uid: String): QrDialogFragment = QrDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_UID, uid) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // full screen transparent background
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        _binding = FragmentQrDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = arguments?.getString(ARG_UID) ?: return
        // genera QR
        val qrDrawable = QrUtils.generateQrDrawable(requireContext(), uid, 512)
        binding.imageQr.setImageDrawable(qrDrawable)
        // click fuori per chiudere
        binding.root.setOnClickListener { dismiss() }
        // impedisci chiusura al click sul QR
        binding.imageQr.setOnClickListener { /* consume */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
