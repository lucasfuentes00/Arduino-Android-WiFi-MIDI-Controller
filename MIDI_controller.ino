#define MIDI_CHANNEL 0                     
#define NOTE_ON_BYTE  (0x90 + MIDI_CHANNEL)
#define NOTE_OFF_BYTE (0x80 + MIDI_CHANNEL)

int buttonPin[3] = {2, 3, 4};            
bool lastButtonState[3] = {HIGH, HIGH, HIGH};  
int lastSensorValue[6] = {0, 0, 0, 0, 0, 0}; 

void setup() {
  Serial.begin(9600); // Use 31250 for standard MIDI. Use 9600 only for debugging.
  
  // Digital inputs
  for (int i = 0; i < 3; i++) {
    pinMode(buttonPin[i], INPUT_PULLUP);
  }

  Serial.println("\nConnected");
}

void loop() {
  bool buttonState[3] = {dRead(0), dRead(1), dRead(2)};
  digitalInterface(buttonState);
  analogInterface();
  updateState(buttonState);
}

bool dRead(int x) {
  return digitalRead(buttonPin[x]);  
}

void updateState(bool buttonState[3]) {
  for (int i = 0; i < 3; i++) {
    lastButtonState[i] = buttonState[i];  
  }
}

void sendMIDI(byte status, byte note, byte velocity) {
  byte message[3] = {status, note, velocity};
  Serial.write(message, 3);  

}

void digitalInterface(bool buttonState[3]) {
  for (int i = 0; i < 3; i++) {
    if (buttonState[i] != lastButtonState[i]) {
      byte note = 60 + i;
      if (buttonState[i] == LOW) {
        sendMIDI(NOTE_ON_BYTE, note, 127); // Button pressed
      } else {
        sendMIDI(NOTE_OFF_BYTE, note, 0);  // Button released
      }
    }
  }
}

void analogInterface() {
  for (int i = 0; i < 6; i++) {
    int sensorValue = analogRead(A0 + i);
    if (abs(sensorValue - lastSensorValue[i]) > 2) {
      byte note = 70 + i;
      //if ((i==0)||(i==2)){
        //sensorValue = (pow(10, sensorValue-1) ) / (10 );
      //}
      byte velocity = map(sensorValue, 0, 675, 0, 127);
      sendMIDI(NOTE_ON_BYTE, note, velocity);
      lastSensorValue[i] = sensorValue;
    }
  }

  delay(100); // Prevent flooding the MIDI bus
}
