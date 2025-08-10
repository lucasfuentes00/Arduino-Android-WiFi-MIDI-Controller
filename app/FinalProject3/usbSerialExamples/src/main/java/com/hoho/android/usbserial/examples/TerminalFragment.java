package com.hoho.android.usbserial.examples;

import static com.hoho.android.usbserial.examples.ui.controller.SendByWifiKt.sendOscMessage;

import static java.lang.Math.abs;

//import static kotlin.text.ScreenFloatValueRegEx.value;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.htrh.studio.rotatybutton.RotaryButton;



public class TerminalFragment extends Fragment implements SerialInputOutputManager.Listener {

    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;

    private final BroadcastReceiver broadcastReceiver;
    private final Handler mainLooper;
    private TextView receiveText, textView;
    private SeekBar seekBar1, seekBar2,seekBar3;
    private RotaryButton knob1,knob2,knob3;
    private ToggleButton toggle1, toggle2, toggle3;
    private Boolean[] update_ui = {false, false, false, false, false, false};
    private int[] current_value = {0, 0, 0, 0, 0, 0};
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    private String IPAddress = "192.168.86.23";


    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();


    int c = 0;
    String sIn = "";

    public TerminalFragment() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceId = getArguments().getInt("device");
        portNum = getArguments().getInt("port");
        baudRate = getArguments().getInt("baud");
        withIoManager = getArguments().getBoolean("withIoManager");
        obtainIP(getContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(getActivity(), broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!connected && (usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted))
            mainLooper.post(this::connect);
    }

    @Override
    public void onPause() {
        if (connected) {
            status("disconnected");
            disconnect();
        }
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);

        receiveText = view.findViewById(R.id.receiveText);
        receiveText.setMovementMethod(new ScrollingMovementMethod());

        textView = view.findViewById(R.id.textView);
        TextView ratingView = view.findViewById(R.id.ratingView);
        seekBar1 = view.findViewById(R.id.seekBar1);
        seekBar2 = view.findViewById(R.id.seekBar2);
        seekBar3 = view.findViewById(R.id.seekBar3);

        knob1 = view.findViewById(R.id.knob1);
        knob2 = view.findViewById(R.id.knob2);
        knob3 = view.findViewById(R.id.knob3);

        toggle1 = view.findViewById(R.id.toggle1);
        toggle2 = view.findViewById(R.id.toggle2);
        toggle3 = view.findViewById(R.id.toggle3);

        seekBar1.setOnSeekBarChangeListener(seekBarListener("70", ratingView));
        seekBar2.setOnSeekBarChangeListener(seekBarListener("71", ratingView));
        seekBar3.setOnSeekBarChangeListener(seekBarListener("72", ratingView));

        knob1.setOnSeekBarChangeListener(circleSeekBarListener("73", ratingView));
        knob2.setOnSeekBarChangeListener(circleSeekBarListener("74", ratingView));
        knob3.setOnSeekBarChangeListener(circleSeekBarListener("75", ratingView));


        setToggleListener(toggle1, "60");
        setToggleListener(toggle2, "61");
        setToggleListener(toggle3, "62");
        return view;
    }
    private void setToggleListener(ToggleButton toggle, String label) {
        toggle.setOnClickListener(v -> {
            if (toggle.isChecked()) {

                sendOscMessage(IPAddress, 5000, label, (float)1.0);
                Log.d("Toggle", label + " ON");

            } else {
                Log.d("Toggle", label + " OFF");
                sendOscMessage(IPAddress, 5000, label, (float)0.0);
            }
        });
    }



    private SeekBar.OnSeekBarChangeListener seekBarListener(String label, TextView ratingView) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ratingView.setText(label +" "+ progress);
                    byte [] chunk = setChunk(label,progress);
                    sendOscMessage(IPAddress, 5000, label, (float) chunk[2] /127);
                    send(label + progress);

                    int index = Integer.parseInt(label.substring(1).trim()) ;
                    update_ui[index] = false;
                    Log.d("Screen", label + " "+ progress);
                } else {
                    // Cambio programático, no hacer nada o hacer solo cosas específicas
                    byte [] chunk = setChunk(label,progress);
                    sendOscMessage(IPAddress, 5000, label, (float) chunk[2] /127);
                    Log.d("USB", "Cambio recibido por USB: " + label + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }
    private boolean isUserChanging = false;

    private RotaryButton.OnCircleSeekBarChangeListener circleSeekBarListener(String label, TextView ratingView) {
        return new RotaryButton.OnCircleSeekBarChangeListener() {
            @Override
            public void onProgressChange(int value) {
                if (isUserChanging) {
                    ratingView.setText(label + " " + value);
                    byte[] chunk = setChunk(label, value);
                    sendOscMessage(IPAddress, 5000, label, (float) chunk[2] /127);

                    int index = Integer.parseInt(label.substring(1).trim()) ;
                    update_ui[index-3] = false;
                } else {
                    Log.d("Programmatic", "Cambio programático en knob " + label + ": " + value);
                    byte[] chunk = setChunk(label, value);
                    sendOscMessage(IPAddress, 5000, label, (float) chunk[2] /127);
                }
            }

            @Override
            public void onStartTrackingTouch(RotaryButton rotaryButton) {
                isUserChanging = true;  // El usuario empieza a tocar el knob
            }

            @Override
            public void onStopTrackingTouch(RotaryButton rotaryButton) {
                isUserChanging = false; // El usuario deja de tocar el knob
            }
        };
    }
    private void obtainIP(Context context) {
        new Thread(() -> {
            try {
                String hostname = "ASUSZENBOOK-PC";
                InetAddress[] addresses = InetAddress.getAllByName(hostname + ".local");
                for (InetAddress addr : addresses) {
                    if (addr instanceof Inet4Address) {
                        IPAddress = addr.getHostAddress();
                        break;
                    }
                }

                Log.d("HOSTNAME", "IP: " + IPAddress);

                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "IP reconocida: " + IPAddress
                                + ", Hostaname: "+ hostname, Toast.LENGTH_LONG).show()
                );
            } catch (Exception e) {
                Log.e("HOSTNAME", "Error resolviendo IP", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "No se pudo resolver el hostname", Toast.LENGTH_SHORT).show()
                );}
        }).start();
    }

    byte[] setChunk(String label, int velocity) {
        int noteNumber = Integer.parseInt(label);

        byte statusByte = (byte) 0x90;
        byte noteByte = (byte) noteNumber;
        byte velocityByte = (byte) velocity;

        return new byte[] { statusByte, noteByte, velocityByte };
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.send_break) {
            if (!connected) {
                Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    usbSerialPort.setBreak(true);
                    Thread.sleep(100);
                    usbSerialPort.setBreak(false);
                    SpannableStringBuilder spn = new SpannableStringBuilder();
                    spn.append("send <break>\n");
                    spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    receiveText.append(spn);
                    scrollToBottom();
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "BREAK failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> receive(data));
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            status("connection lost: " + e.getMessage());
            disconnect();
        });
    }

    private void connect() {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values())
            if (v.getDeviceId() == deviceId)
                device = v;
        if (device == null) {
            status("connection failed: device not found");
            return;
        }

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if (driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if (driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if (usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            Intent intent = new Intent(INTENT_ACTION_GRANT_USB);
            intent.setPackage(getActivity().getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            status("connection failed: " + (!usbManager.hasPermission(driver.getDevice()) ? "permission denied" : "open failed"));
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if (withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
            connected = true;
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
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

    private void send(String str) {
        if (!connected) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str + '\n').getBytes();
            SpannableStringBuilder spn = new SpannableStringBuilder();
            spn.append("send " + str + " bytes\n");
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            scrollToBottom();

            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    private void receive1(byte[] data) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        c = c + data.length;
        int index = -1;

        if (data.length > 0) {
            String str = new String(data);
            sIn = sIn + str;
            index = sIn.indexOf("\r");
        }

        if (index != -1) {
            receiveText.append(sIn);
            scrollToBottom();
            c = 0;
            sIn = "";
        }
    }
    private void receive(byte[] data) {
        try {
            buffer.write(data);
            while (buffer.size() >= 3) {
                byte[] packet = buffer.toByteArray();
                byte[] chunk = Arrays.copyOfRange(packet, 0, 3);


                processThreeBytes(chunk);

                buffer.reset();
                if (packet.length > 3) {
                    buffer.write(packet, 3, packet.length - 3);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void processThreeBytes(byte[] chunk) {
        int b1 = chunk[0] & 0xFF;
        int b2 = chunk[1] & 0xFF;
        int b3 = chunk[2] & 0xFF;

        String message = String.format("Received 3 bytes: [%d, %d, %d]\n", b1, b2, b3);
        receiveText.append(message);
        scrollToBottom();
        updateUI(b2,b3);
    }
        void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
        scrollToBottom();
    }
    private void checkUpdateUI(int i,int b3){
        if (i<3) {
            String seekBarID = "seekBar" + (i + 1);
            int resID = getResources().getIdentifier(seekBarID, "id", getContext().getPackageName());
            SeekBar seekBar = getView().findViewById(resID);
            current_value[i] = seekBar.getProgress();
            if (Math.abs(current_value[0] -b3) < 10) {
                update_ui[i] = Boolean.TRUE;
            }
            if(update_ui[i]) {
                seekBar.setProgress(b3);
            }
        }
        if (i>=3){
            i = i-3;
            String knobID = "knob" + (i + 1); // i starting from 0
            int resID = getResources().getIdentifier(knobID, "id", getContext().getPackageName());
            RotaryButton knob = getView().findViewById(resID);
            current_value[i] = knob.getProgress();
            if (Math.abs(current_value[0] -b3) < 10) {
                update_ui[i] = Boolean.TRUE;
            }
            if(update_ui[i]) {
                knob.setProgress(b3);
            }
        }
    }
    private void updateUI(int b2, int b3) {


        switch (b2) {
            case 70:
                checkUpdateUI(0,b3);
                break;
            case 71:
                checkUpdateUI(1,b3);
                break;
            case 72:
                checkUpdateUI(2,b3);;
                break;
            case 73:
                checkUpdateUI(3,b3);
                break;
            case 74:
                checkUpdateUI(4,b3);
                break;
            case 75:
                checkUpdateUI(5,b3);
                break;
            case 60:
                toggle1.setChecked(b3 != 0);
                sendOscMessage(IPAddress, 5000, Integer.toString(b2), (float) b3);
                break;
            case 61:
                toggle2.setChecked(b3 != 0);
                sendOscMessage(IPAddress, 5000, Integer.toString(b2), (float) b3);
                break;
            case 62:
                toggle3.setChecked(b3 != 0);
                sendOscMessage(IPAddress, 5000, Integer.toString(b2), (float) b3);
                break;
        }
    }

    private void scrollToBottom() {
        receiveText.post(() -> {
            int scrollAmount = receiveText.getLayout().getLineTop(receiveText.getLineCount()) - receiveText.getHeight();
            if (scrollAmount > 0)
                receiveText.scrollTo(0, scrollAmount);
            else
                receiveText.scrollTo(0, 0);
        });
    }

}
