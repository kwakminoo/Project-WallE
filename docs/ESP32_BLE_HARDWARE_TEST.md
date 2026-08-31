# WOLI ESP32 BLE 실기기 검증

이 문서는 ESP32-WROOM-32와 Android 앱의 BLE, 거치/손 접근 입력, 서보 잠금 상태를 실제 부품으로 확인하는 절차입니다. 실제 부품이 없으면 아래 결과를 검증 완료로 표시하지 말고 Android의 `WOLI-Simulator`를 별도 시연 경로로 사용합니다.

## 연결과 안전

| 용도 | ESP32 핀 | 기본 설정 |
|---|---:|---|
| 잠금 서보 신호 | GPIO 18 | `LOCK_SERVO_DEGREES=90`, `UNLOCK_SERVO_DEGREES=10` |
| 스마트폰 거치 스위치 | GPIO 19 | `INPUT_PULLUP`, active-low |
| 손 접근 디지털 입력 | GPIO 21 | `INPUT_PULLUP`, active-low |
| 배터리 ADC | 미사용 (`-1`) | 현재는 `battery=100` 보고 |

- 거치 스위치는 GPIO 19와 GND 사이에 연결합니다. 손 접근 센서는 센서 사양에 맞춰 VCC와 GND를 먼저 연결하고, 센서 GND와 ESP32 GND를 공통으로 만든 뒤 디지털 출력만 GPIO 21에 연결합니다. 출력은 반드시 3.3 V GPIO 호환이어야 하며 5 V 출력 센서는 레벨 시프터를 거칩니다.
- GPIO 19/GPIO 21은 기본적으로 pull-up이므로, active-low 설정에서는 GND 쪽 입력이 감지 상태입니다. 센서 종류가 바뀌면 `MOUNT_SENSOR_ACTIVE_LOW`, `HAND_SENSOR_ACTIVE_LOW`, `SENSOR_STABLE_SAMPLES`만 조정합니다.
- 서보 전원은 안정적인 5 V를 사용하고 ESP32와 GND를 공통으로 연결합니다. 서보를 ESP32 3.3 V 핀에서 구동하지 마세요. 배선 변경과 각도 보정은 전원을 끈 상태에서 하고, 처음 잠금 시험은 스마트폰을 넣지 않은 상태에서 합니다.
- 이 검증 범위는 BLE, 센서, 서보 잠금만 다룹니다.

## BLE 프로토콜

| 항목 | 값 |
|---|---|
| Device name | `WOLI-DT01` |
| Service | `7e7a0001-1f5f-4c2b-9b6f-2d1b7f4f0100` |
| Command | `7e7a0002-1f5f-4c2b-9b6f-2d1b7f4f0100` — UTF-8 WRITE |
| Status | `7e7a0003-1f5f-4c2b-9b6f-2d1b7f4f0100` — READ + NOTIFY |

명령은 `LOCK`, `UNLOCK`, `START`, `SESSION_END`, `STOP`, `STATUS`, `CAL_LOCK=90`, `CAL_UNLOCK=10`입니다.

- `START`: 집중 세션을 활성화하고 잠급니다.
- `SESSION_END`: 정상 집중 완료를 알리고 세션을 비활성화한 뒤 잠금을 풉니다.
- `STOP`: 비정상 종료/호환용 명령입니다. 앱의 중도 종료는 기존 미션 흐름을 먼저 따라야 합니다.
- `STATUS`: 현재 상태를 즉시 보냅니다.

상태 형식은 `mount=1;lock=1;hand=0;battery=100;session=1`입니다. 각 값은 `mount`(거치), `lock`(잠금), `hand`(손 접근), `session`(집중 세션)이 `0` 또는 `1`이고 `battery`는 0~100입니다. 상태 변경/명령 처리 시 즉시 Notify되고, 변경이 없어도 약 1초마다 heartbeat가 올 수 있습니다.

## STEP 1–15: Android 앱 실기기 검증

1. **ESP32 USB 연결**: ESP32-WROOM-32를 USB로 연결하고 보드가 인식되는지 확인합니다.
2. **펌웨어 업로드**: `firmware`에서 `pio pkg install`, `pio run -t upload`를 실행합니다.
3. **Serial Monitor 115200**: `pio device monitor -b 115200`을 열어 로그를 확인합니다.
4. **Advertising 확인**: `[WOLI][BOOT] firmware ready`와 `[WOLI][BLE] advertising started` 뒤에 `WOLI-DT01`이 검색되는지 확인합니다.
5. **Android 앱 실행**: Android 실기기에서 월이 앱을 열고 Bluetooth 권한을 허용합니다.
6. **월이 기기 검색**: 기기 연결 화면에서 `WOLI-DT01`을 찾습니다.
7. **연결**: 기기를 선택해 연결하고, Service Discovery와 Status Notify 등록이 끝날 때까지 기다립니다.
8. **STATUS 확인**: 하드웨어 진단 화면에서 `STATUS`를 실행합니다. 표시된 mount/lock/hand/battery/session 값과 Serial의 `[WOLI][STATUS]` payload가 일치해야 합니다.
9. **스마트폰 거치 센서 테스트**: GPIO 19 스위치를 동작시켜 `mount=0`과 `mount=1` Notify가 즉시 반영되는지 확인합니다.
10. **START 전송**: 앱의 최소 집중 시간은 5분입니다. 시연에서는 시작 직후 화면의 완료 기능을 쓰거나, 5분 타이머로 시작합니다. 예상 상태는 `lock=1`, `session=1`이며 Serial에는 `[WOLI][CMD] START`, `[WOLI][SESSION] active=1`, `[WOLI][LOCK] 0 -> 1`가 표시됩니다.
11. **손 접근**: GPIO 21 센서를 감지 상태로 만듭니다. `hand=1` Notify, Android 손 접근 경고 화면, 세션 `handWarningCount +1`이 보여야 합니다.
12. **손 계속 유지**: 감지 상태를 유지합니다. heartbeat가 와도 경고 횟수는 추가로 증가하지 않아야 합니다.
13. **손 제거**: 센서를 비감지 상태로 돌립니다. `hand=0` Notify 뒤 앱은 버튼 조작 없이 기본 집중 눈 화면으로 돌아와야 합니다.
14. **다시 손 접근**: 다시 `hand=1`로 바꿉니다. 이번에는 경고 횟수가 정확히 한 번 더 증가해야 합니다.
15. **세션 정상 종료**: 5분 타이머가 0이 되도록 기다리거나 시연용 완료 기능을 사용합니다. 앱은 `SESSION_END`를 보내고, `lock=0`, `session=0`, `FocusCompleteScreen`, 세션 리포트가 차례로 보여야 합니다.

## 연결 해제와 재연결

집중 중 앱의 연결을 끊어도 앱 타이머와 세션 기록은 유지되어야 합니다. Serial에서 `[WOLI][BLE] client disconnected; advertising restart enabled`를 확인한 뒤, 앱에서 같은 `WOLI-DT01`을 다시 연결합니다. 재연결 후 Status Notify 등록과 최초 READ(READ 실패 시 `STATUS`) 동기화로 현재 mount/lock/hand/battery/session 값이 다시 표시되어야 합니다. 정상 완료 중 연결이 끊겼다면 재연결 뒤 보류된 `SESSION_END`가 전송되어 `lock=0;session=0`으로 복구되는지 확인합니다. 무한 재연결을 기다리지 말고, 실패 시 Bluetooth를 한 번 껐다 켠 뒤 수동 재연결합니다.

대표 Serial 로그는 다음과 같습니다.

```text
[WOLI][BOOT] firmware ready
[WOLI][BLE] advertising started
[WOLI][BLE] client connected
[WOLI][CMD] START
[WOLI][SESSION] active=1
[WOLI][HAND] 0 -> 1
[WOLI][STATUS] mount=1;lock=1;hand=1;battery=100;session=1
[WOLI][BLE] client disconnected
```

## nRF Connect로 ESP32만 분리 검증

Android 앱 문제가 아닌지 확인할 때는 nRF Connect 같은 일반 BLE GATT 클라이언트를 사용합니다.

1. `WOLI-DT01`을 Scan 후 Connect합니다.
2. WOLI Service를 열고 Status Characteristic에서 **Read**를 실행한 뒤 **Notify**를 구독합니다.
3. Command Characteristic에 UTF-8 텍스트로 `STATUS`를 Write합니다. Status가 즉시 갱신되는지 확인합니다.
4. `START`를 Write해 `lock=1;session=1`을 확인합니다.
5. `SESSION_END`를 Write해 `lock=0;session=0`을 확인합니다.
6. `LOCK`, `UNLOCK`도 각각 Write해 잠금 값과 Notify를 확인합니다.

기대 payload 예시는 다음과 같습니다.

```text
mount=1;lock=1;hand=0;battery=100;session=1
```

센서/서보를 연결하지 않은 개발 보드에서는 pull-up 기본 상태에 따라 `mount=0`, `hand=0`이 정상이며, 배터리 ADC를 사용하지 않으면 `battery=100`이 정상입니다.
