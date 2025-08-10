package com.hoho.android.usbserial.examples.ui.controller

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun sendOscMessage(serverIP: String, port: Int, address: String, data: Float) {
    Thread {
        try {
            val message = data.toString()
            val socket = DatagramSocket()

            val addressBytes = (address + "\u0000").toByteArray()
            val addressPaddedLength = (addressBytes.size + 3) / 4 * 4
            val paddedAddressBytes = ByteArray(addressPaddedLength)
            System.arraycopy(addressBytes, 0, paddedAddressBytes, 0, addressBytes.size)

            val typeTag = ",s\u0000\u0000".toByteArray()

            val messageBytes = (message + "\u0000").toByteArray()
            val messagePaddedLength = (messageBytes.size + 3) / 4 * 4
            val paddedMessageBytes = ByteArray(messagePaddedLength)
            System.arraycopy(messageBytes, 0, paddedMessageBytes, 0, messageBytes.size)

            val packetBytes = ByteArray(paddedAddressBytes.size + typeTag.size + paddedMessageBytes.size)
            var pos = 0
            System.arraycopy(paddedAddressBytes, 0, packetBytes, pos, paddedAddressBytes.size)
            pos += paddedAddressBytes.size
            System.arraycopy(typeTag, 0, packetBytes, pos, typeTag.size)
            pos += typeTag.size
            System.arraycopy(paddedMessageBytes, 0, packetBytes, pos, paddedMessageBytes.size)

            val inetAddress = InetAddress.getByName(serverIP)
            val packet = DatagramPacket(packetBytes, packetBytes.size, inetAddress, port)
            socket.send(packet)

            Log.d("UDP", "Sent string message: $message to $address")

            socket.close()
        } catch (e: Exception) {
            Log.e("UDP", "Error sending OSC message: ${e.message}", e)
        }
    }.start()
}

fun sendOscTimestamp(serverIP: String, port: Int, oscAddress: String = "latency") {
    Thread {
        try {
            val timestamp = System.currentTimeMillis().toDouble()// en ms
            val message = timestamp.toString()
            Log.d("HOSTNAME", "Envio: $message")

            val socket = DatagramSocket()

            val addressBytes = (oscAddress + "\u0000").toByteArray()
            val addressPaddedLength = (addressBytes.size + 3) / 4 * 4
            val paddedAddressBytes = ByteArray(addressPaddedLength)
            System.arraycopy(addressBytes, 0, paddedAddressBytes, 0, addressBytes.size)

            val typeTag = ",s\u0000\u0000".toByteArray()

            val messageBytes = (message + "\u0000").toByteArray()
            val messagePaddedLength = (messageBytes.size + 3) / 4 * 4
            val paddedMessageBytes = ByteArray(messagePaddedLength)
            System.arraycopy(messageBytes, 0, paddedMessageBytes, 0, messageBytes.size)

            val packetBytes = ByteArray(paddedAddressBytes.size + typeTag.size + paddedMessageBytes.size)
            var pos = 0
            System.arraycopy(paddedAddressBytes, 0, packetBytes, pos, paddedAddressBytes.size)
            pos += paddedAddressBytes.size
            System.arraycopy(typeTag, 0, packetBytes, pos, typeTag.size)
            pos += typeTag.size
            System.arraycopy(paddedMessageBytes, 0, packetBytes, pos, paddedMessageBytes.size)

            val inetAddress = InetAddress.getByName(serverIP)
            val packet = DatagramPacket(packetBytes, packetBytes.size, inetAddress, port)
            socket.send(packet)

            Log.d("UDP", "Sent timestamp: $timestamp to $oscAddress")

            socket.close()
        } catch (e: Exception) {
            Log.e("UDP", "Error sending OSC timestamp: ${e.message}", e)
        }
    }.start()
}




