/**
 * WOLI ESP32 firmware skeleton (shell stage).
 * Roles: BLE server, mount detect, servo lock/unlock.
 * No full BLE GATT yet — compile-ready stubs only.
 */
#include <Arduino.h>

#ifndef SERVO_PIN
#define SERVO_PIN 18
#endif

#ifndef LIMIT_SWITCH_PIN
#define LIMIT_SWITCH_PIN 19
#endif

static bool locked = false;

static void setLock(bool enable) {
  // ponytail: PWM 서보 제어는 하드웨어 캘리브 후 교체. 지금은 상태 플래그만.
  locked = enable;
  Serial.printf("[WOLI] lock=%s\n", locked ? "ON" : "OFF");
}

void setup() {
  Serial.begin(115200);
  pinMode(LIMIT_SWITCH_PIN, INPUT_PULLUP);
  Serial.println("[WOLI] firmware shell ready");
  setLock(false);
}

void loop() {
  const bool mounted = digitalRead(LIMIT_SWITCH_PIN) == LOW;
  static bool prev = false;
  if (mounted != prev) {
    Serial.printf("[WOLI] mount=%s\n", mounted ? "YES" : "NO");
    prev = mounted;
  }
  delay(50);
}
