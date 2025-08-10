package com.hoho.android.usbserial.examples.ui.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hoho.android.usbserial.examples.databinding.FragmentControllerBinding
import com.hoho.android.usbserial.examples.DevicesFragment
import com.hoho.android.usbserial.examples.R

class ControllerFragment : Fragment() {

    private var _binding: FragmentControllerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(ControllerViewModel::class.java)

        _binding = FragmentControllerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            parentFragmentManager.beginTransaction()
                .add(R.id.fragment, DevicesFragment(), "devices")
                .commit()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}