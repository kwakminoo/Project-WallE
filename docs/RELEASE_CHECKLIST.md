# WOLI 1차 릴리즈 체크리스트

월이 앱과 ESP32 펌웨어를 시연 가능한 상태로 고정하기 위한 체크리스트입니다.

## 1. Android 빌드

- [ ] `android/local.properties`에 실제 Android SDK 경로가 있다.
- [ ] 디버그 빌드가 성공한다.

```bash
cd android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat lint
```

macOS/Linux에서는 `./gradlew :app:testDebugUnitTest :app:assembleDebug lint`를 사용합니다.

- [ ] APK 위치를 확인한다.

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## 2. 릴리즈 서명

`local.properties`나 환경 변수로만 서명 정보를 관리하고, 키스토어와 비밀번호는 git에 올리지 않습니다.

권장 환경 변수:

```bash
export WOLI_STORE_FILE=/absolute/path/woli-release.jks
export WOLI_STORE_PASSWORD=...
export WOLI_KEY_ALIAS=woli
export WOLI_KEY_PASSWORD=...
```

릴리즈 빌드 설정을 추가한 뒤 다음 명령으로 확인합니다.

```bash
cd android
./gradlew :app:assembleRelease
```

## 3. Android 실기기 권한

- [ ] 알림 접근: 설정 > 알림 접근 > 월이 허용
- [ ] 앱 알림: Android 13 이상에서 월이 알림 허용
- [ ] 배터리 제한: 설정 > 배터리 최적화에서 월이를 제한 없음 또는 예외로 설정
- [ ] 전화 감지: 전화 상태 권한 허용
- [ ] 발신자 식별: 통화 기록 권한은 기기/OS 정책에 따라 선택 허용
- [ ] 전화 제어: 받기/거절 API가 허용되는 기기에서만 검증
- [ ] 연락처: 중요 연락처 매칭에 필요한 경우만 허용
- [ ] Bluetooth: ESP32 실기기 연결 전 허용

앱 내부 확인 위치:

- 설정 > 집중 알림 기준
- 설정 > 알림 / TTS 검증
- 설정 > 하드웨어 검증
- 집중 시작 플로우 > 집중 알림 전달

## 4. 앱별 답장 검증

앱마다 RemoteInput 지원 방식이 다릅니다. 아래는 실기기에서 직접 확인해야 합니다.

- [ ] SMS 또는 기본 메시지 앱
- [ ] KakaoTalk
- [ ] Telegram
- [ ] WhatsApp

검증 기준:

- [ ] 설정 > 집중 알림 기준 기본값이 `균형` 모드로 표시된다.
- [ ] 일반 카카오톡/문자/DM은 기본적으로 TTS 안내되지 않는다.
- [ ] 중요 연락처의 카카오톡/문자/DM은 TTS 안내된다.
- [ ] 중요 연락처 + 긴급 표현 메시지는 긴급 알림으로 표시된다.
- [ ] 광고/쿠폰/좋아요/댓글/묶음/재생 중 알림은 TTS 안내되지 않는다.
- [ ] `시연` 모드에서 답장 가능한 테스트 메시지가 안내된다.
- [ ] 알림 제목/본문이 개인정보 마스킹 없이 과도하게 저장되지 않는다.
- [ ] 월이가 TTS로 중요 알림만 안내한다.
- [ ] 음성 명령 `답장`, `보내`, `취소`가 의도대로 동작한다.
- [ ] 전송 성공/실패가 설정 > 알림 / TTS 검증에 남는다.

## 5. ESP32 펌웨어

- [ ] PlatformIO가 설치되어 있다.
- [ ] 빌드가 성공한다.

```bash
cd firmware
pio pkg install
pio run
```

- [ ] 업로드가 성공한다.

```bash
pio run -t upload
pio device monitor
```

- [ ] BLE 이름이 `WOLI-DT01`로 보인다.
- [ ] 앱의 설정 > 하드웨어 검증에서 `STATUS`, `LOCK`, `UNLOCK`, `START`, `SESSION_END`, `CAL_LOCK`, `CAL_UNLOCK` 명령이 동작한다.
- [ ] `START` 후 Status가 `lock=1;session=1`을, 정상 완료의 `SESSION_END` 후 `lock=0;session=0`을 보고한다.
- [ ] Status Characteristic의 READ + NOTIFY로 mount/lock/hand/battery/session이 실제 상태와 일치한다.
- [ ] GPIO 19 거치 입력과 GPIO 21 손 접근 입력이 debounce 후 즉시 Notify된다. 손을 유지해도 세션 경고 횟수는 증가하지 않고, 손을 떼면 집중 눈 화면으로 복귀한다.
- [ ] 집중 중 연결을 끊고 다시 연결한 뒤 `STATUS` 동기화가 정상이며 앱 타이머/세션 기록이 유지된다.
- [ ] nRF Connect에서 Command Write와 Status Read + Notify를 별도로 확인했다.

세부 배선, 안전 주의, STEP 1–15, nRF Connect 절차는 [ESP32 BLE 실기기 검증](ESP32_BLE_HARDWARE_TEST.md)을 따른다.

## 6. 시연 전 고정

- [ ] 스마트폰 1대, 예비 스마트폰 1대, USB-C 케이블, 보조배터리를 준비한다.
- [ ] ESP32 전원 공급이 흔들리지 않는다.
- [ ] 서보 잠금 각도가 스마트폰을 무리하게 누르지 않는다.
- [ ] 발표용 연락처와 테스트 메시지만 사용한다.
- [ ] 개인정보가 들어간 실제 알림은 방해 금지 또는 별도 계정으로 차단한다.
- [ ] 네트워크가 없어도 핵심 시연이 가능한 시뮬레이션 경로를 준비한다.
