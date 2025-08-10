package com.hoho.android.usbserial.examples;

import static android.app.PendingIntent.getActivity;
import static com.hoho.android.usbserial.examples.TerminalFragment.INTENT_ACTION_GRANT_USB;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

public class DeviceManager {

    private static final Intent INTENT_ACTION_GRANT_USB = null;
    // Static fields to store the selected device information
    private static int selectedDeviceId = -1;
    private static UsbDevice selectedDevice;
    private static int selectedPort = -1;
    private static int selectedBaudRate = -1;
    private static boolean selectedWithIoManager = false;

    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;
    private boolean connected = false;

    private final UsbManager usbManager;
    private final int deviceId;
    private final int portNum;
    private final int baudRate;
    private final boolean withIoManager;
    public String Status = "disconnected";

    // UsbPermission enum for tracking permission state
    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private UsbPermission usbPermission = UsbPermission.Unknown;
    private static Context context ;

    // Static method to set selected device info
    public static void setSelectedDevice(
            Context context,
            int deviceId,
            UsbDevice device,
            int port,
            int baudRate,
            boolean withIoManager
    ) {
        selectedDeviceId = deviceId;
        selectedDevice = device;
        selectedPort = port;
        selectedBaudRate = baudRate;
        selectedWithIoManager = withIoManager;
        saveDeviceInfo(context);  // Save device info using the passed context
    }

    // Constructor to initialize DeviceManager
    public DeviceManager(Context context, int deviceId, int portNum, int baudRate, boolean withIoManager) {
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.deviceId = deviceId;
        this.portNum = portNum;
        this.baudRate = baudRate;
        this.withIoManager = withIoManager;
    }

    // Method to connect the USB device

    public void connect() {
        UsbDevice device = null;

        // Search for the device with the given deviceId
        for (UsbDevice v : usbManager.getDeviceList().values()) {
            if (v.getDeviceId() == deviceId) {
                device = v;
                break;
            }
        }

        if (device == null) {
            status("Connection failed: device not found");
            Status = "Connection failed: device not found";
            return;
        }

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if (driver == null) {
            status("Connection failed: no driver for device");
            Status = "Connection failed: no driver for device";
            return;
        }
        if (driver.getPorts().size() < portNum) {
            status("Connection failed: not enough ports at device");
            Status = "Connection failed: not enough ports at device";
            return;
        }

        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());

        if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            Intent intent = new Intent(INTENT_ACTION_GRANT_USB);
            intent.setPackage(context.getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }

        // After permission is granted, attempt to open the device again
        if (usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice())) {
                status("Connection failed: permission denied");
                Status = "Connection failed: permission denied";


            } else {
                status("Connection failed: open failed");
                Status = "Connection failed: open failed";
            }
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            try {
                usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            } catch (UnsupportedOperationException e) {
                status("Unsupported setParameters");
                Status = "Unsupported setParameters";
            }

            if (withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, (SerialInputOutputManager.Listener) this);
                usbIoManager.start();
            }

            status("Connected");
            Status= "Connected";
            connected = true;

            saveDeviceInfo(context);

        } catch (Exception e) {
            status("Connection failed: " + e.getMessage());
            disconnect();
        }
    }



    // Method to disconnect the device
    public void disconnect() {
        connected = false;
        Status= "Disconnected";
        if (usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    // Method to send data to the device
    public void send(String data) {
        if (!connected) return;
        try {
            byte[] byteData = (data + '\n').getBytes();
            usbSerialPort.write(byteData, 2000);
        } catch (IOException e) {
            // Handle sending error
        }
    }

    // Method to read data from the device
    public void read() {
        if (!connected) return;
        try {
            byte[] buffer = new byte[8192];
            int len = usbSerialPort.read(buffer, 2000);
            // Handle received data
        } catch (IOException e) {
            // Handle read error
        }
    }



    // Helper method to display status messages
    private void status(String message) {
        // Display the connection status (could be a log or Toast)
        System.out.println(message);
    }
    public static void saveDeviceInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("DeviceInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("deviceId", selectedDeviceId);
        editor.putInt("portNum", selectedPort);
        editor.putInt("baudRate", selectedBaudRate);
        editor.putBoolean("withIoManager", selectedWithIoManager);
        // You can also serialize and save other complex objects if needed
        editor.apply();
    }
    public static void loadDeviceInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("DeviceInfo", Context.MODE_PRIVATE);
        selectedDeviceId = sharedPreferences.getInt("deviceId", -1); // Default value is -1 if not found
        selectedPort = sharedPreferences.getInt("portNum", -1);
        selectedBaudRate = sharedPreferences.getInt("baudRate", -1);
        selectedWithIoManager = sharedPreferences.getBoolean("withIoManager", false);
    }
}
