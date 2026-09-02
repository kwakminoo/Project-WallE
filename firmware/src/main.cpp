/**
 * WOLI ESP32 firmware.
 *
 * BLE GATT protocol:
 * - service  7e7a0001-1f5f-4c2b-9b6f-2d1b7f4f0100
 * - command  7e7a0002-1f5f-4c2b-9b6f-2d1b7f4f0100  WRITE
 * - status   7e7a0003-1f5f-4c2b-9b6f-2d1b7f4f0100  READ/NOTIFY
 *
 * Status payload: mount=1;lock=0;hand=0;battery=100;session=0
 * Commands: LOCK, UNLOCK, START, SESSION_END, STOP, STATUS, HAND_NEAR,
 * HAND_FAR, CAL_LOCK=90, CAL_UNLOCK=10
 *
 * Hand approach is driven by the phone camera over BLE (HAND_NEAR/HAND_FAR).
 * N20 wheel motors retreat on HAND_NEAR and return on HAND_FAR or session end.
 */
#include <Arduino.h>
#include <ESP32Servo.h>
#include <NimBLEDevice.h>

#ifndef SERVO_PIN
#define SERVO_PIN 13
#endif

#ifndef LIMIT_SWITCH_PIN
#define LIMIT_SWITCH_PIN -1
#endif

#ifndef MOTOR_LEFT_IN1
#define MOTOR_LEFT_IN1 16
#endif

#ifndef MOTOR_LEFT_IN2
#define MOTOR_LEFT_IN2 17
#endif

#ifndef MOTOR_RIGHT_IN3
#define MOTOR_RIGHT_IN3 18
#endif

#ifndef MOTOR_RIGHT_IN4
#define MOTOR_RIGHT_IN4 19
#endif

#ifndef MOVE_TIME_MS
#define MOVE_TIME_MS 600
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

  void syncInitial() {
    stableValue = readRaw();
    candidateValue = stableValue;
    candidateCount = SENSOR_STABLE_SAMPLES;
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
#if LIMIT_SWITCH_PIN >= 0
static DebouncedInput mountSensor(LIMIT_SWITCH_PIN, MOUNT_SENSOR_ACTIVE_LOW == 1);
#endif
static bool locked = false;
static bool mounted = false;
static bool handNear = false;
static bool sessionActive = false;
static bool isRetreated = false;
static uint8_t batteryPercent = 100;
static int lockDegrees = LOCK_SERVO_DEGREES;
static int unlockDegrees = UNLOCK_SERVO_DEGREES;

static bool motorsEnabled() {
  return MOTOR_LEFT_IN1 >= 0 && MOTOR_LEFT_IN2 >= 0 && MOTOR_RIGHT_IN3 >= 0 &&
         MOTOR_RIGHT_IN4 >= 0;
}

static void stopMotors() {
  if (!motorsEnabled()) {
    return;
  }
  digitalWrite(MOTOR_LEFT_IN1, LOW);
  digitalWrite(MOTOR_LEFT_IN2, LOW);
  digitalWrite(MOTOR_RIGHT_IN3, LOW);
  digitalWrite(MOTOR_RIGHT_IN4, LOW);
}

static void moveBackward() {
  if (!motorsEnabled()) {
    return;
  }
  Serial.println("[WOLI][MOTOR] retreat backward");
  digitalWrite(MOTOR_LEFT_IN1, LOW);
  digitalWrite(MOTOR_LEFT_IN2, HIGH);
  digitalWrite(MOTOR_RIGHT_IN3, LOW);
  digitalWrite(MOTOR_RIGHT_IN4, HIGH);
  delay(MOVE_TIME_MS);
  stopMotors();
}

static void moveForward() {
  if (!motorsEnabled()) {
    return;
  }
  Serial.println("[WOLI][MOTOR] return forward");
  digitalWrite(MOTOR_LEFT_IN1, HIGH);
  digitalWrite(MOTOR_LEFT_IN2, LOW);
  digitalWrite(MOTOR_RIGHT_IN3, HIGH);
  digitalWrite(MOTOR_RIGHT_IN4, LOW);
  delay(MOVE_TIME_MS);
  stopMotors();
}

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
         ";battery=" + String(batteryPercent) +
         ";session=" + (sessionActive ? "1" : "0");
}

static void notifyStatus(bool logStatus = true) {
  if (statusCharacteristic == nullptr) {
    return;
  }

  batteryPercent = readBatteryPercent();
  const String payload = statusPayload();
  statusCharacteristic->setValue(payload.c_str());
  if (statusCharacteristic->getSubscribedCount() > 0) {
    statusCharacteristic->notify();
  }
  if (logStatus) {
    Serial.printf("[WOLI][STATUS] %s\n", payload.c_str());
  }
}

static void applyServoAngle(int angle) {
  lockServo.write(constrain(angle, 0, 180));
}

static void releaseRetreatIfNeeded() {
  if (!isRetreated) {
    return;
  }
  moveForward();
  isRetreated = false;
}

static void setHandNear(bool near, bool allowRetreatMotion) {
  if (handNear == near) {
    return;
  }

  const bool wasHandNear = handNear;
  handNear = near;
  Serial.printf("[WOLI][HAND] %d -> %d\n", wasHandNear ? 1 : 0, handNear ? 1 : 0);

  if (!allowRetreatMotion || !sessionActive) {
    notifyStatus();
    return;
  }

  if (handNear && !isRetreated) {
    moveBackward();
    isRetreated = true;
  } else if (!handNear && isRetreated) {
    moveForward();
    isRetreated = false;
  }
  notifyStatus();
}

static void setSessionActive(bool active) {
  if (sessionActive == active) {
    return;
  }

  sessionActive = active;
  Serial.printf("[WOLI][SESSION] active=%d\n", sessionActive ? 1 : 0);
}

static void endSession() {
  setSessionActive(false);
  releaseRetreatIfNeeded();
  handNear = false;
  setLock(false);
}

static void setLock(bool enable) {
  const bool wasLocked = locked;
  locked = enable;
  applyServoAngle(enable ? lockDegrees : unlockDegrees);
  if (wasLocked != locked) {
    Serial.printf("[WOLI][LOCK] %d -> %d angle=%d\n", wasLocked ? 1 : 0,
                  locked ? 1 : 0, enable ? lockDegrees : unlockDegrees);
  }
  notifyStatus();
}

static bool updateCalibration(const String &command, const char *prefix, int *target) {
  if (!command.startsWith(prefix)) {
    return false;
  }

  const int value = command.substring(strlen(prefix)).toInt();
  *target = constrain(value, 0, 180);
  Serial.printf("[WOLI][CAL] %s%d\n", prefix, *target);
  setLock(locked);
  return true;
}

static void handleCommand(const String &rawCommand) {
  String command = rawCommand;
  command.trim();
  command.toUpperCase();

  Serial.printf("[WOLI][CMD] %s\n", command.c_str());

  if (command == "START") {
    isRetreated = false;
    handNear = false;
    setSessionActive(true);
    setLock(true);
    return;
  }
  if (command == "SESSION_END" || command == "STOP") {
    endSession();
    return;
  }
  if (command == "LOCK") {
    setLock(true);
    return;
  }
  if (command == "UNLOCK") {
    setLock(false);
    return;
  }
  if (command == "HAND_NEAR") {
    setHandNear(true, true);
    return;
  }
  if (command == "HAND_FAR") {
    setHandNear(false, true);
    return;
  }
  if (command == "STATUS") {
    notifyStatus();
    return;
  }
  if (updateCalibration(command, "CAL_LOCK=", &lockDegrees)) {
    return;
  }
  if (updateCalibration(command, "CAL_UNLOCK=", &unlockDegrees)) {
    return;
  }

  Serial.printf("[WOLI][CMD] unknown=%s\n", command.c_str());
}

class CommandCallbacks : public NimBLECharacteristicCallbacks {
 public:
  void onWrite(NimBLECharacteristic *characteristic) override {
    const std::string value = characteristic->getValue();
    handleCommand(String(value.c_str()));
  }
};

class ServerCallbacks : public NimBLEServerCallbacks {
 public:
  void onConnect(NimBLEServer *) override {
    Serial.println("[WOLI][BLE] client connected");
  }

  void onDisconnect(NimBLEServer *) override {
    Serial.println("[WOLI][BLE] client disconnected; advertising restart enabled");
  }
};

static void setupBle() {
  NimBLEDevice::init(DEVICE_NAME);
  NimBLEServer *server = NimBLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());
  server->advertiseOnDisconnect(true);
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
  if (advertising->start()) {
    Serial.println("[WOLI][BLE] advertising started");
  } else {
    Serial.println("[WOLI][BLE] advertising start failed");
  }
}

static void setupMotors() {
  if (!motorsEnabled()) {
    Serial.println("[WOLI][BOOT] wheel motors disabled");
    return;
  }

  pinMode(MOTOR_LEFT_IN1, OUTPUT);
  pinMode(MOTOR_LEFT_IN2, OUTPUT);
  pinMode(MOTOR_RIGHT_IN3, OUTPUT);
  pinMode(MOTOR_RIGHT_IN4, OUTPUT);
  stopMotors();
  Serial.printf("[WOLI][BOOT] wheel motors ready moveMs=%d\n", MOVE_TIME_MS);
}

void setup() {
  Serial.begin(115200);
  Serial.println("[WOLI][BOOT] starting");
#if LIMIT_SWITCH_PIN >= 0
  pinMode(LIMIT_SWITCH_PIN, INPUT_PULLUP);
  mountSensor.syncInitial();
  mounted = mountSensor.stableValue;
#else
  mounted = true;
#endif
#if BATTERY_ADC_PIN >= 0
  pinMode(BATTERY_ADC_PIN, INPUT);
#endif

  batteryPercent = readBatteryPercent();
  Serial.printf("[WOLI][BOOT] mount=%d hand=%d battery=%u\n", mounted ? 1 : 0,
                handNear ? 1 : 0, static_cast<unsigned>(batteryPercent));

  ESP32PWM::allocateTimer(0);
  lockServo.setPeriodHertz(50);
  lockServo.attach(SERVO_PIN, SERVO_MIN_US, SERVO_MAX_US);
  setLock(false);
  setupMotors();
  setupBle();
  Serial.println("[WOLI][BOOT] firmware ready");
}

void loop() {
#if LIMIT_SWITCH_PIN >= 0
  if (mountSensor.update()) {
    const bool wasMounted = mounted;
    mounted = mountSensor.stableValue;
    Serial.printf("[WOLI][MOUNT] %d -> %d\n", wasMounted ? 1 : 0, mounted ? 1 : 0);
    notifyStatus();
  }
#endif

  static uint32_t lastHeartbeat = 0;
  const uint32_t now = millis();
  if (now - lastHeartbeat >= STATUS_INTERVAL_MS) {
    lastHeartbeat = now;
    notifyStatus(false);
  }

  delay(25);
}
