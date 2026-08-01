# Project-WallE / 월이(WOLI)

스마트폰을 로봇 머리에 거치하면 집중 모드로 전환되는 **디지털 디톡스 로봇** 프로젝트입니다.  
중요한 연락은 남기고, 불필요한 연결만 잠시 끊도록 돕는 집중 동반 로봇입니다.

> 슬로건: **집중할 땐 함께, 쉴 땐 자유롭게.**

---

## 프로젝트 한눈에 보기

| 구분 | 내용 |
|---|---|
| 제품명 | 월이 (WOLI) |
| 앱 | Android (Kotlin + Jetpack Compose) |
| 펌웨어 | ESP32 (Arduino / PlatformIO, NimBLE) |
| 통신 | BLE (Bluetooth Low Energy) |
| 현재 단계 | **UI 껍데기** — 화면 흐름만 구현, BLE·타이머·센서 기능 미연결 |

### 핵심 아이디어

1. 앱에서 집중 시간·중요 연락처를 설정한다.
2. 스마트폰을 월이 머리 거치대에 **가로**로 올린다.
3. BLE 연결 후 물리적 잠금이 작동하고, 화면은 **눈만 보이는 집중 모드**가 된다.
4. 10분 단위로 남은 시간을 잠깐 표시하고, 중요 연락·손 접근 시에만 표정/TTS로 안내한다.
5. 중도 해제 시 리듬 미션 등 마찰 장치를 거친다.

자세한 기획은 루트의 `디지털_디톡스_로봇_월이_기획안.md`와 `SW 예 상시나리오.png`를 참고하세요.

---

## 저장소 구조

```text
Project-WallE/
├── android/                 # Android 앱 (Compose UI 껍데기)
├── firmware/                # ESP32 PlatformIO 스켈레톤
├── 디지털_디톡스_로봇_월이_기획안.md
├── SW 예 상시나리오.png
├── HW 구상도.png
├── 월E 구상도.png
└── README.md
```

---

## 개발 시작하기 (처음 clone 한 경우)

### 0) 공통

- Git, GitHub 계정
- 이 저장소 clone:

```bash
git clone https://github.com/kwakminoo/Project-WallE.git
cd Project-WallE
```

### 1) Android 앱

#### 필수 설치

1. **JDK 17** (Temurin / Oracle 등)
2. **Android Studio** (권장: Ladybug 이상) 또는 Android SDK Command-line Tools
3. Android SDK
   - `platforms;android-36` (또는 35+)
   - `build-tools;35.0.0` 이상
   - Platform-Tools

#### SDK 경로 설정

`android/local.properties` 파일을 만들고 SDK 경로를 적습니다.  
(이 파일은 git에 올리지 않습니다.)

```properties
sdk.dir=C\:\\Users\\<YOU>\\AppData\\Local\\Android\\Sdk
```

macOS/Linux 예:

```properties
sdk.dir=/Users/<YOU>/Library/Android/sdk
```

#### 의존성 / 패키지 (Gradle이 자동 다운로드)

앱 모듈(`android/app/build.gradle.kts`)에 포함된 주요 의존성:

| 용도 | 패키지 |
|---|---|
| UI | Jetpack Compose BOM, Material3, Material Icons |
| 네비게이션 | `androidx.navigation:navigation-compose` |
| 생명주기 | `lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose` |
| 로컬 저장(예정) | `datastore-preferences` |
| 비동기 | `kotlinx-coroutines-android` |
| BLE 권한 | Manifest에 선언 (구현은 이후) |

설치/동기화:

```bash
cd android
./gradlew.bat dependencies
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:testDebugUnitTest
```

Android Studio를 쓰는 경우:

1. **Open** → `Project-WallE/android` 폴더 선택
2. Gradle Sync 완료 대기
3. 에뮬레이터 또는 실기기에서 `app` Run

#### 구현된 UI 껍데기 화면

세로(집중 전)

- 홈 / 통계 / 미션 / 설정
- 집중 시간 설정
- 월이 기기 연결
- 중요 연락처
- 스마트폰 거치 안내
- 화면 껍데기 갤러리 (설정에서 진입)

가로(집중 모드)

- 기본 눈 화면
- 남은 시간 잠깐 표시
- 중요 연락
- 손 접근 경고
- 집중 완료
- 중도 해제 확인
- 리듬 미션
- 세션 리포트

집중 눈 화면 하단의 데모 칩으로 가로 화면 상태를 오갈 수 있습니다.

### 2) ESP32 펌웨어

#### 필수 설치

1. [VS Code](https://code.visualstudio.com/) + **PlatformIO** 확장  
   또는 [PlatformIO Core CLI](https://platformio.org/install/cli)
2. USB 드라이버 (보드에 따라 CP210x / CH340)

#### 의존성

`firmware/platformio.ini` 기준:

- platform: `espressif32`
- board: `esp32dev` (ESP32-WROOM-32)
- framework: `arduino`
- lib: `h2zero/NimBLE-Arduino`

빌드/업로드:

```bash
cd firmware
pio pkg install
pio run
pio run -t upload
pio device monitor
```

현재 `src/main.cpp`는 시리얼 로그 + 리미트 스위치 감지 스텁만 포함합니다.

### 3) (선택) 설계 도구

| 영역 | 도구 |
|---|---|
| UI·UX | Figma |
| 3D 모델링 | Fusion 360 / Onshape |
| 렌더링 | Blender |

---

## MVP 범위 (참고)

**1차 MVP:** 앱 실행, 집중 시간, BLE, 서보 잠금, 눈 화면, 10분 단위 표시, 중요 전화, 완료/리포트  

**현재 저장소:** 위 화면의 **UI 껍데기** + 펌웨어 스켈레톤 + 기획 문서

---

## GitHub

- 원격 저장소: https://github.com/kwakminoo/Project-WallE

대용량/비관련 파일(`TNC Web Re.zip` 등)은 `.gitignore`로 제외합니다.
