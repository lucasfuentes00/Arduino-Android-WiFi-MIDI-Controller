# 🎛️ Arduino-Android–UDP conection (Max for Live Communication System)

## 📌 Overview  
This project implements a **bidirectional communication system** between an **Android smartphone**, an **Arduino microcontroller**, and a **receiver in Max for Live (Ableton Live)**.  
The system enables real-time interaction between mobile devices, hardware, and music software with **low latency** and **reliable wireless communication**.

## ✨ Features  
- 📱 **Android App**: Sends and receives control messages.  
- 🤖 **Arduino Firmware**: Handles serial and wireless data communication.  
- 🎶 **Max for Live Device**: Receives and processes the messages for music control.  
- ⚡ **Low Latency**: ~50 ms average delay using UDP.  
- 🔄 **Bidirectional Communication**: Both Android and Arduino exchange data in real time.  
- 🌐 **Wireless Connection**: Seamless integration with a computer over Wi-Fi.

## 🛠️ Components  
- **Android Application** – developed in Kotlin, uses UDP for communication.  
- **Arduino Project** – compatible with Arduino Uno and ESP modules for Wi-Fi.  
- **Max for Live Receiver** – custom device to process incoming data inside Ableton Live.

## 📂 Project Structure  
