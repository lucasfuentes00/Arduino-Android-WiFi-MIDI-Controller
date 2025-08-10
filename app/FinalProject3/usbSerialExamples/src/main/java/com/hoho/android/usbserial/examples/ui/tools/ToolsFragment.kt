package com.hoho.android.usbserial.examples.ui.tools

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.hoho.android.usbserial.examples.databinding.FragmentToolsBinding
import com.hoho.android.usbserial.examples.ui.controller.sendOscMessage
import com.htrh.studio.rotatybutton.RotaryButton
import java.net.InetAddress

import androidx.lifecycle.lifecycleScope
import com.hoho.android.usbserial.examples.ui.controller.sendOscTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address

class ToolsFragment : Fragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    private var ipAddress = "192.168.86.23"
    private val port = 5000

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        obtainIP()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        val root = binding.root

        binding.seekBar1.setOnSeekBarChangeListener(seekBarListener("76"))
        binding.knob1.setOnSeekBarChangeListener(knobListener("77"))
        setToggleListener(binding.toggle1, "63")

        return root
    }

    private fun setToggleListener(toggle: ToggleButton, label: String) {
        toggle.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) 1 else 0
            sendOscMessage(ipAddress, port, label, value.toFloat())
            Log.d("Toggle", "$label = $value")
        }
    }

    private fun seekBarListener(label: String) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    sendOscMessage(ipAddress, port, label, progress.toFloat()/127)
                    Log.d("SeekBar", "$label = $progress")
                    if (progress ==127){
                        sendOscTimestamp(ipAddress,port)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun knobListener(label: String) =
        object : RotaryButton.OnCircleSeekBarChangeListener {
            override fun onProgressChange(value: Int) {
                sendOscMessage(ipAddress, port, label, value.toFloat()/127)
                Log.d("Knob", "$label = $value")
            }

            override fun onStartTrackingTouch(rotaryButton: RotaryButton?) {}
            override fun onStopTrackingTouch(rotaryButton: RotaryButton?) {}
        }

    private fun obtainIP() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val hostname = "ASUSZENBOOK-PC"

                ipAddress  = withContext(Dispatchers.IO) {
                    InetAddress.getAllByName("$hostname.local")
                        .firstOrNull { it is Inet4Address }?.hostAddress.toString()
                }

                //ipAddress = address.hostAddress
                //ipAddress = "192.168.86.23"
                Log.d("HOSTNAME", "IP: $ipAddress")

                Toast.makeText(
                    requireContext(),
                    "IP reconocida: $ipAddress, Hostname: $hostname",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("HOSTNAME", "Error resolviendo IP", e)
                Toast.makeText(
                    requireContext(),
                    "No se pudo resolver el hostname",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
