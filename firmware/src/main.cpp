/**
 * WOLI ESP32 firmware.
 *
 * BLE GATT protocol:
 * - service  7e7a0001-1f5f-4c2b-9b6f-2d1b7f4f0100
 * - command  7e7a0002-1f5f-4c2b-9b6f-2d1b7f4f0100  WRITE
 * - status   7e7a0003-1f5f-4c2b-9b6f-2d1b7f4f0100  READ/NOTIFY
 *
 * Status payload: mount=1;lock=0;hand=0;battery=100
 * Commands: LOCK, UNLOCK, START, STOP, STATUS, CAL_LOCK=90, CAL_UNLOCK=10
 */
#include <Arduino.h>
#include <ESP32Servo.h>
#include <NimBLEDevice.h>

#ifndef SERVO_PIN
#define SERVO_PIN 18
#endif

#ifndef LIMIT_SWITCH_PIN
#define LIMIT_SWITCH_PIN 19
#endif

#ifndef HAND_SENSOR_PIN
#define HAND_SENSOR_PIN 21
#endif

#ifndef BATTERY_ADC_PIN
#define BATTERY_ADC_PIN -1
#endif

#ifndef SERVO_MIN_US
#define SERVO_MIN_US 500
#endif

#ifndef SERVO_MAX_US
#define SERVO_MAX_US 2400
#endif

#ifndef LOCK_SERVO_DEGREES
#define LOCK_SERVO_DEGREES 90
#endif

#ifndef UNLOCK_SERVO_DEGREES
#define UNLOCK_SERVO_DEGREES 10
#endif

#ifndef MOUNT_SENSOR_ACTIVE_LOW
#define MOUNT_SENSOR_ACTIVE_LOW 1
#endif

#ifndef HAND_SENSOR_ACTIVE_LOW
#define HAND_SENSOR_ACTIVE_LOW 1
#endif

#ifndef SENSOR_STABLE_SAMPLES
#define SENSOR_STABLE_SAMPLES 4
#endif

#ifndef STATUS_INTERVAL_MS
#define STATUS_INTERVAL_MS 1000
#endif

static const char *DEVICE_NAME = "WOLI-DT01";
static const char *SERVICE_UUID = "7e7a0001-1f5f-4c2b-9b6f-2d1b7f4f0100";
static const char *COMMAND_UUID = "7e7a0002-1f5f-4c2b-9b6f-2d1b7f4f0100";
static const char *STATUS_UUID = "7e7a0003-1f5f-4c2b-9b6f-2d1b7f4f0100";

struct DebouncedInput {
  uint8_t pin;
  bool activeLow;
  bool stableValue = false;
  bool candidateValue = false;
  uint8_t candidateCount = 0;

  DebouncedInput(uint8_t inputPin, bool inputActiveLow)
      : pin(inputPin), activeLow(inputActiveLow) {}

  bool readRaw() const {
    const bool high = digitalRead(pin) == HIGH;
    return activeLow ? !high : high;
  }

  bool update() {
    const bool next = readRaw();
    if (next == candidateValue) {
      if (candidateCount < SENSOR_STABLE_SAMPLES) {
        candidateCount++;
      }
    } else {
      candidateValue = next;
      candidateCount = 1;
    }

    if (candidateCount >= SENSOR_STABLE_SAMPLES && stableValue != candidateValue) {
      stableValue = candidateValue;
      return true;
    }
    return false;
  }
};

static NimBLECharacteristic *statusCharacteristic = nullptr;
static Servo lockServo;
static DebouncedInput mountSensor(LIMIT_SWITCH_PIN, MOUNT_SENSOR_ACTIVE_LOW == 1);
static DebouncedInput handSensor(HAND_SENSOR_PIN, HAND_SENSOR_ACTIVE_LOW == 1);
static bool locked = false;
static bool mounted = false;
static bool handNear = false;
static uint8_t batteryPercent = 100;
static int lockDegrees = LOCK_SERVO_DEGREES;
static int unlockDegrees = UNLOCK_SERVO_DEGREES;

static uint8_t readBatteryPercent() {
#if BATTERY_ADC_PIN >= 0
  const int raw = analogRead(BATTERY_ADC_PIN);
  const int percent = map(raw, 0, 4095, 0, 100);
  return constrain(percent, 0, 100);
#else
  return batteryPercent;
#endif
}

static String statusPayload() {
  return String("mount=") + (mounted ? "1" : "0") +
         ";lock=" + (locked ? "1" : "0") +
         ";hand=" + (handNear ? "1" : "0") +
         ";battery=" + String(batteryPercent);
}

static void notifyStatus() {
  if (statusCharacteristic == nullptr) {
    return;
  }

  batteryPercent = readBatteryPercent();
  const String payload = statusPayload();
  statusCharacteristic->setValue(payload.c_str());
  statusCharacteristic->notify();
  Serial.printf("[WOLI] status %s\n", payload.c_str());
}

static void applyServoAngle(int angle) {
  lockServo.write(constrain(angle, 0, 180));
}

static void setLock(bool enable) {
  locked = enable;
  applyServoAngle(enable ? lockDegrees : unlockDegrees);
  Serial.printf("[WOLI] lock=%s angle=%d\n", locked ? "ON" : "OFF", enable ? lockDegrees : unlockDegrees);
  notifyStatus();
}

static bool updateCalibration(const String &command, const char *prefix, int *target) {
  if (!command.startsWith(prefix)) {
    return false;
  }

  const int value = command.substring(strlen(prefix)).toInt();
  *target = constrain(value, 0, 180);
  Serial.printf("[WOLI] calibration %s%d\n", prefix, *target);
  setLock(locked);
  return true;
}

static void handleCommand(const String &rawCommand) {
  String command = rawCommand;
  command.trim();
  command.toUpperCase();

  Serial.printf("[WOLI] command=%s\n", command.c_str());

  if (command == "LOCK" || command == "START") {
    setLock(true);
  } else if (command == "UNLOCK" || command == "STOP") {
    setLock(false);
  } else if (command == "STATUS") {
    notifyStatus();
  } else if (updateCalibration(command, "CAL_LOCK=", &lockDegrees)) {
    notifyStatus();
  } else if (updateCalibration(command, "CAL_UNLOCK=", &unlockDegrees)) {
    notifyStatus();
  } else {
    Serial.printf("[WOLI] unknown command=%s\n", command.c_str());
  }
}

class CommandCallbacks : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic *characteristic) override {
    const std::string value = characteristic->getValue();
    handleCommand(String(value.c_str()));
  }
};

static void setupBle() {
  NimBLEDevice::init(DEVICE_NAME);
  NimBLEServer *server = NimBLEDevice::createServer();
  NimBLEService *service = server->createService(SERVICE_UUID);

  NimBLECharacteristic *commandCharacteristic = service->createCharacteristic(
      COMMAND_UUID,
      NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR);
  commandCharacteristic->setCallbacks(new CommandCallbacks());

  statusCharacteristic = service->createCharacteristic(
      STATUS_UUID,
      NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
  statusCharacteristic->setValue(statusPayload().c_str());

  service->start();

  NimBLEAdvertising *advertising = NimBLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->start();

  Serial.println("[WOLI] BLE advertising started");
}

void setup() {
  Serial.begin(115200);
  pinMode(LIMIT_SWITCH_PIN, INPUT_PULLUP);
  pinMode(HAND_SENSOR_PIN, INPUT_PULLUP);
#if BATTERY_ADC_PIN >= 0
  pinMode(BATTERY_ADC_PIN, INPUT);
#endif

  lockServo.setPeriodHertz(50);
  lockServo.attach(SERVO_PIN, SERVO_MIN_US, SERVO_MAX_US);
  setLock(false);

  mounted = mountSensor.readRaw();
  handNear = handSensor.readRaw();
  setupBle();
  Serial.println("[WOLI] firmware ready");
}

void loop() {
  bool changed = false;
  changed = mountSensor.update() || changed;
  changed = handSensor.update() || changed;

  if (mountSensor.stableValue != mounted) {
    mounted = mountSensor.stableValue;
    changed = true;
  }
  if (handSensor.stableValue != handNear) {
    handNear = handSensor.stableValue;
    changed = true;
  }
  if (changed) {
    notifyStatus();
  }

  static uint32_t lastHeartbeat = 0;
  const uint32_t now = millis();
  if (now - lastHeartbeat > STATUS_INTERVAL_MS) {
    lastHeartbeat = now;
    notifyStatus();
  }

  delay(25);
}
