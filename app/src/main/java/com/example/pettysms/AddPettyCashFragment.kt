package com.example.pettysms

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.example.pettysms.databinding.FragmentAddPettyCashBinding
import com.example.pettysms.databinding.FragmentEditTransactionBinding
import com.example.pettysms.databinding.FragmentPettyCashBinding

class AddPettyCashFragment : DialogFragment() {

    private var _binding: FragmentAddPettyCashBinding? = null
    private lateinit var toolbar: Toolbar

    private val binding get() = _binding!!
    private var functionality = "Add"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PettySMS)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        dialog.window?.attributes?.windowAnimations = R.style.FullscreenDialogAnimation
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddPettyCashBinding.inflate(inflater, container, false)
        toolbar = binding.addPettyCashToolbar




        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "$functionality Petty Cash"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            closeDialog()
        }


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun closeDialog() {
        dismiss()
    }

    companion object {
    }
}