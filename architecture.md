# Librera Reader 아키텍처 분석

> **작성일**: 2025-05-25
> **대상 프로젝트**: Librera Reader (Android E-Book Reader)
> **분석 범위**: 리더기(reader) 부분을 중심으로 한 전체 아키텍처

---

## 1. 프로젝트 모듈 구조

Librera는 Gradle 기반의 멀티모듈 Android 프로젝트이다. `settings.gradle.kts`와 `build.gradle.kts`를 통해 다음 모듈이 관리된다.

### 1.1 핵심 애플리케이션 모듈

| 모듈명 | 유형 | 설명 |
|--------|------|------|
| `:app` | `androidApplication` | 메인 애플리케이션 모듈. 모든 UI(Activity/Fragment), 비즈니스 로직, 데이터 모델이 포함됨. |

### 1.2 라이브러리 모듈

| 모듈명 | 유형 | 설명 |
|--------|------|------|
| `:libPro` | `androidLibrary` | PRO 버전 전용 라이브러리. `mobi.librera.libPro` 네임스페이스를 사용하며, F-Droid 버전과 분리된 빌드에 사용됨. (빈 의존성) |
| `:libReflow` | `androidLibrary` | 텍스트 리플로우(Reflow) 관련 기능 라이브러리. `mobi.librera.libReflow` 네임스페이스 사용. (빈 의존성) |
| `:libDepFree` | `androidLibrary` | Google Play Services가 포함된 "Free" 빌드용 라이브러리. 광고(AdMob) 관련 의존성을 포함. (`play-services-ads`, `user-messaging-platform`) |
| `:libDepPro` | `androidLibrary` | "Pro" 빌드용 라이브러리. Google Drive 연동, RAR 압축 해제, 인앱 리뷰 등 고급 기능 의존성 포함. (`play-services-auth`, `google-api-services-drive`, `junrar`, `review`) |

### 1.3 빌드 및 도우미 모듈

| 모듈명 | 유형 | 설명 |
|--------|------|------|
| `:Builder` | `androidApplication` | 빌드 스크립트 및 폰트 등 빌드 리소스 관리 모듈. MuPDF와의 링크 스크립트(`link_to_mupdf_*.sh`) 포함. |

### 1.4 비활성화된 모듈

| 모듈명 | 상태 | 설명 |
|--------|------|------|
| `:composeApp` | 주석 처리됨 | Jetpack Compose 기반의 미래 UI 모듈 (현재 미사용) |
| `:shared` | 주석 처리됨 | Kotlin Multiplatform 공유 모듈 (현재 미사용) |

### 1.5 빌드 플레이버 (Product Flavors)

`app` 모듈은 8개의 `productFlavor`를 정의하여 여러 개의 별도 앱을 하나의 코드베이스에서 빌드한다.

```
fdroid        -> "Librera FD" (오픈소스 버전, 광고/구글 서비스 없음)
librera       -> "Librera" (기본 버전)
pro           -> "Librera PRO" (유료 버전)
pdf_classic   -> "PDF Reader" (브랜딩 변경)
ebooka        -> "Book Reader" (브랜딩 변경)
pdf_v2        -> "PDF Reader" (2번째 브랜딩)
tts_reader    -> "TTS Reader" (음성 리딩 중심)
epub_reader   -> "Epub Reader" (EPUB 중심)
```

---

## 2. 리더기 핵심 패키지 구조

Java 소스 코드는 `app/src/main/java/com/foobnix/` 경로 아래에 있으며, 리더기 기능과 관련된 주요 패키지는 다음과 같다.

### 2.1 애플리케이션 루트 및 진입점

| 패키지/클래스 | 설명 |
|--------------|------|
| `com.foobnix.OpenerActivity` | 외부 Intent(파일 열기, 공유)를 처리하는 엔트리 Activity. 파일을 탐색하여 적절한 뷰어로 라우팅한다. |
| `com.foobnix.LibreraApp` | Application 클래스. 전역 Context 초기화 및 라이프사이클 관리. |

### 2.2 UI 및 메인 화면 (`ui2`)

| 패키지/클래스 | 설명 |
|--------------|------|
| `com.foobnix.ui2.MainTabs2` | 앱의 메인 화면(Activity). `ViewPager` + `SlidingTabLayout`으로 구성된 라이브러리 화면. |
| `com.foobnix.ui2.fragment.*` | 메인 화면의 각 탭 Fragment (`RecentFragment2`, `BrowseFragment2`, `SearchFragment2`, `BookmarksFragment2`, `OpdsFragment2`, `PrefFragment2` 등) |
| `com.foobnix.ui2.AdsFragmentActivity` | 광고가 필요한 Activity의 기반 클래스. |

### 2.3 문서 렌더링 및 뷰어 (`pdf.search`, `pdf.info`)

| 패키지/클래스 | 설명 |
|--------------|------|
| `com.foobnix.pdf.search.activity.HorizontalViewActivity` | **수평(가로) 페이지 뷰어 Activity**. EPUB/FB2/MOBI 등 텍스트 기반 포맷의 기본 뷰어. ViewPager 기반. |
| `org.ebookdroid.ui.viewer.VerticalViewActivity` | **수직(세로) 스크롤 뷰어 Activity**. PDF/DjVu 등 이미지 기반 포맷의 기본 뷰어. |
| `com.foobnix.pdf.info.wrapper.DocumentController` | 뷰어의 핵심 추상 컨트롤러. 페이지 이동, 줌, 탭 영역 처리, 밝기 조절 등 모든 뷰어의 공통 동작을 정의하는 추상 클래스. |
| `com.foobnix.pdf.info.view.*` | 커스텀 View 모음 (`DrawView`, `BorderTextView`, `UnderlineImageView` 등) |

### 2.4 포맷별 처리기

| 패키지 | 설명 |
|--------|------|
| `com.foobnix.libfb2` | **FB2 (FictionBook 2) 포맷** 처리. XML 파싱, 섹션 추출, HTML 변환 등. |
| `com.foobnix.libmobi` | **MOBI/AZW 포맷** 처리. `libmobi` JNI 라이브러리와 연동하여 MOBI 파일을 파싱하고 HTML 콘텐츠를 추출. |
| `com.foobnix.mobi.parser` | MOBI/AZW 내부 파서. `MobiParserIS` 등이 파일 구조를 해석. |
| `com.foobnix.ext` | 다양한 포맷(RTF, DOCX, TXT 등)의 확장 처리 및 캐싱 유틸리티. |

### 2.5 데이터 및 설정 (`model`, `dao2`)

| 패키지 | 설명 |
|--------|------|
| `com.foobnix.model.*` | 전역 데이터 모델 (`AppState`, `AppProfile`, `AppBook`, `AppBookmark`, `AppData`, `AppSP`) |
| `com.foobnix.dao2.*` | GreenDAO ORM 기반 데이터베이스 엔티티 및 DAO (`FileMeta`, `FileMetaDao`, `DictMeta`, `DictMetaDao`, `DaoMaster`, `DaoSession`) |
| `com.foobnix.ui2.AppDB` | 앱 전체의 데이터베이스 접근 싱글톤. `FileMeta` CRUD 및 검색 기능 제공. |

### 2.6 부가 기능

| 패키지 | 설명 |
|--------|------|
| `com.foobnix.tts.*` | TTS(Text-To-Speech) 서비스 및 컨트롤러. |
| `com.foobnix.hypen.*` | 하이픈네이션(단어 분리) 처리. |
| `com.foobnix.sync.*` | 설정 동기화 (Google Drive 연동). |
| `com.foobnix.opds.*` | OPDS(Open Publication Distribution System) 카탈로그 검색 및 다운로드. |
| `com.foobnix.drive.*` | Google Drive 클라우드 파일 동기화. |
| `com.foobnix.zipmanager.*` | ZIP/RAR 등 압축 파일 내 도서 처리. |
| `com.foobnix.work.*` | WorkManager 기반 백그라운드 작업 (도서 색인, 동기화). |

---

## 3. 주요 Activity/Fragment 흐름

앱 실행부터 책 열기까지의 전체 사용자 흐름은 다음과 같다.

```
[AndroidManifest Entry Point]
    |
    v
[OpenerActivity] ---(외부 파일 열기 Intent)---> [파일 탐색 및 복사] ---(성공)---> [ExtUtils.openFile()]
    |
    v
[MainTabs2] (앱 직접 실행 시)
    |
    +---> [ViewPager] 탭 전환
    |       |
    |       +---> [RecentFragment2]   : 최근 읽은 책 목록
    |       +---> [BrowseFragment2]   : 파일 시스템 및 SD카드 탐색
    |       +---> [SearchFragment2]   : 내부 라이브러리 검색
    |       +---> [BookmarksFragment2]: 북마크 목록
    |       +---> [OpdsFragment2]     : OPDS 온라인 카탈로그
    |       +---> [PrefFragment2]     : 설정 (DrawerLayout 내부)
    |
    v
[사용자가 책 선택]
    |
    v
[ExtUtils.openFile(context, fileMeta)]
    |
    v
[Format 분기]
    |
    +---> [PDF, DjVu, XPS, CBZ, CBR] ---> [VerticalViewActivity] (세로 스크롤/페이지 뷰어)
    |
    +---> [EPUB, FB2, MOBI, AZW, TXT, RTF, DOCX, ODT] ---> [HorizontalViewActivity] (수평 뷰어)
    |
    v
[뷰어 Activity]
    |
    +---> [DocumentController] 초기화
    |       +---> [ImageExtractor] 페이지 렌더링
    |       +---> [OutlineHelper] 목차(Outline) 로딩
    |
    v
[사용자 리딩 화면]
```

### 3.1 상태 전이 상세

1. **MainTabs2.onCreate()**: `AppProfile.init()`, `AppState.get().loadInit()`을 통해 전역 설정 및 프로필을 초기화. `ViewPager`와 `TabsAdapter2`를 설정하여 라이브러리 탭을 렌더링. 만약 `isOpenLastBook`이 true이면 마지막 읽은 책을 자동으로 엶.

2. **책 선택**: 사용자가 파일을 선택하면 `FileMeta` 객체가 생성. `ExtUtils.openFile()`이 호출되어 포맷에 따라 적절한 Activity로 Intent를 전달.

3. **VerticalViewActivity(세로 뷰어)**: `org.ebookdroid.ui.viewer.VerticalViewActivity`를 상속. EBookDroid의 뷰어 프레임워크를 사용. MuPDF를 통해 페이지를 Bitmap으로 렌더링하고 SurfaceView 또는 ImageView에 표시.

4. **HorizontalViewActivity(수평 뷰어)**: `AdsFragmentActivity`를 상속. 내부적으로 `VerticalViewPager`를 사용하며, 텍스트 포맷의 경우 **페이지를 HTML로 변환 -> 이미지로 렌더링하여 표시**하는 방식을 사용.

---

## 4. 문서 렌더링 엔진

### 4.1 MuPDF 연동

Librera는 문서 렌더링의 핵심 엔진으로 **MuPDF** (Artifex Software, AGPL License)를 사용한다. MuPDF는 C/C++ 기반 라이브러리이므로 JNI(Java Native Interface)를 통해 Android에서 호출된다.

- **JNI 연동**: `Builder/link_to_mupdf_*.sh` 스크립트를 통해 MuPDF 소스와 `jniLibs`를 연결.
- **사용 위치**: `org.ebookdroid.core.codec.*` 패키지가 MuPDF의 JNI 래퍼 역할을 하며, `CodecDocument`, `CodecPage` 인터페이스를 제공.

### 4.2 포맷별 렌더링 경로

앱은 파일 확장자에 따라 두 가지 주요 렌더링 경로로 분기된다.

#### 경로 A: 이미지/페이지 기반 렌더링 (PDF, DjVu, XPS, CBZ, CBR)

```
[File]
  |
  v
[VerticalViewActivity]
  |
  v
[DocumentController]
  |
  v
[ImageExtractor] ---(MuPDF JNI)---> [CodecDocument / CodecPage]
  |
  v
[Bitmap 생성] ---> [ImageView 표시]
```

- **PDF/DjVu**: MuPDF가 직접 페이지를 비트맵으로 렌더링.
- **CBZ/CBR**: 압축 해제 후 이미지 파일들을 순서대로 페이지처럼 표시.
- **동작**: 사용자가 스크롤하거나 페이지를 넘기면 `ImageExtractor`가 필요한 페이지 번호를 MuPDF에 요청하여 비트맵을 생성하고, 이를 이미지 캐시(`IMG` 및 Glide)에 저장하여 재사용.

#### 경로 B: 텍스트 기반 렌더링 (EPUB, FB2, MOBI, AZW, TXT, RTF, DOCX, ODT)

```
[File]
  |
  v
[HorizontalViewActivity]
  |
  v
[DocumentController]
  |
  +---> [포맷별 Parser]
  |       +---> EPUB: ZIP 압축 해제 -> HTML/XHTML 파싱
  |       +---> FB2: XML 파싱 -> HTML 변환 (com.foobnix.libfb2)
  |       +---> MOBI/AZW: libmobi 파싱 -> HTML 변환 (com.foobnix.libmobi)
  |       +---> TXT/RTF/DOCX/ODT: 확장 모듈(ext)을 통해 HTML 변환
  |
  v
[HTML 생성]
  |
  v
[WebView 또는 HTML -> 이미지 렌더링]
  |
  v
[페이지 표시]
```

- **텍스트 포맷의 핵심 아이디어**: 텍스트 기반 포맷들은 내부적으로 HTML로 변환된 후, **MuPDF가 이 HTML을 페이지 크기에 맞춰 비트맵으로 렌더링**하여 독자에게 이미지 기반 페이지처럼 보이게 한다.
- **CSS 스타일링**: `BookCSS` 클래스가 전역 폰트, 색상, 정렬, 마진, 줄 간격 등을 관리. 사용자가 설정한 테마(라이트/다크/잉크)가 CSS로 적용됨.
- **Reflow(리플로우)**: 화면 크기에 따라 텍스트를 재배치하는 기능. `libReflow` 모듈과 연계하여 동작.

### 4.3 렌더링 흐름 상세 다이어그램

```
                    +---------------------+
                    |      FileMeta       |
                    | (파일 경로, 포맷)     |
                    +----------+----------+
                               |
                               v
                  +---------------------------+
                  |   ExtUtils.openFile()     |
                  | (포맷에 따라 Activity 분기) |
                  +------+-------------+------+
                         |             |
          +--------------+             +-------------+
          |                                           |
          v                                           v
+-----------------------+                 +-----------------------+
| VerticalViewActivity  |                 | HorizontalViewActivity|
| (PDF, DjVu, CBZ...)   |                 | (EPUB, FB2, MOBI...)  |
+-----------+-----------+                 +-----------+-----------+
            |                                           |
            v                                           v
+-----------+-----------+                 +-----------+-----------+
| DocumentController    |                 | DocumentController    |
| (onGoToPage, onZoom...) |                 | (onGoToPage, onZoom...) |
+-----------+-----------+                 +-----------+-----------+
            |                                           |
            v                                           v
+-----------+-----------+                 +-----------+-----------+
| ImageExtractor        |                 | [Format Parser]       |
| (페이지 요청, 캐싱)     |                 | (FB2/MOBI/EPUB ->    |
|                       |                 |  HTML 변환)           |
+-----------+-----------+                 +-----------+-----------+
            |                                           |
            v                                           v
+-----------+-----------+                 +-----------+-----------+
| MuPDF JNI (CodecPage) |                 | MuPDF HTML Renderer   |
| (Bitmap 렌더링)          |                 | (HTML -> Bitmap)      |
+-----------+-----------+                 +-----------+-----------+
            |                                           |
            v                                           v
+-----------+-----------+                 +-----------+-----------+
|     Bitmap/Drawable   |                 |     Bitmap/Drawable   |
|     (이미지 캐시에 저장)|                 |     (이미지 캐시에 저장)|
+-----------------------+                 +-----------------------+
```

---

## 5. 주요 부가 기능 아키텍처

### 5.1 TTS (Text-To-Speech)

| 구성 요소 | 설명 |
|-----------|------|
| `TTSEngine` | TTS 엔진 싱글톤. Android `TextToSpeech` 클래스를 래핑. 엔진 초기화, 음성 속도/피치 설정, MP3 모드 지원. |
| `TTSService` | Foreground Service. 백그라운드에서 책을 읽어주는 핵심 서비스. `AudioManager`를 통해 오디오 포커스를 획득하고, Bluetooth 헤드셋 미디어 버튼 이벤트를 수신. `WakeLock`을 유지하여 화면이 꺼져도 계속 재생. |
| `TTSControlsView` | 뷰어 화면에 오버레이되는 TTS 컨트롤 UI. 재생/일시정지, 타이머, 속도 조절. |
| `TTSNotification` | Foreground Notification을 표시하여 사용자가 다른 앱에서도 TTS를 제어할 수 있게 함. |
| 동작 흐름 | `DocumentController`에서 현재 페이지 텍스트를 추출 -> `TTSService`가 해당 텍스트를 Android TTS 엔진에 전달 -> 음성 출력. 특정 구두점(`TTS_PUNCUATIONS`)을 기준으로 문장 단위로 나누어 읽음. |

### 5.2 하이픈네이션 (Hyphenation)

| 구성 요소 | 설명 |
|-----------|------|
| `DefaultHyphenator` | TextJustify-Android 라이브러리 기반의 하이픈네이션 엔진. Trie 자료구조를 사용하여 빠르게 단어 내 음절 분리 지점을 찾음. |
| `HyphenPattern` | 각 언어별(영어, 독일어 등) 하이픈 패턴 데이터를 enum으로 관리. |
| `HypenUtils` | 하이픈 적용 유틸리티. HTML 콘텐츠를 파싱하여 하이픈을 삽입. |
| 동작 | 텍스트 포맷이 HTML로 변환되는 과정에서 `BookCSS.isAutoHypens`가 true이면 `HypenUtils`를 통해 하이픈이 적용된 HTML을 생성. |

### 5.3 동기화 (Sync)

| 구성 요소 | 설명 |
|-----------|------|
| `GFile` | Google Drive 동기화 핵심 클래스. `GoogleSignIn` 및 `Drive API`를 사용하여 설정 파일(JSON)과 서적 메타데이터를 동기화. |
| `GSync` | 동기화 로직을 담당하는 헬퍼 클래스. |
| `SynctornizatoinWorker` | WorkManager 기반 동기화 Worker. 백그라운드에서 주기적으로 동기화를 수행. |
| 동기화 대상 | `AppState`(설정), `BookCSS`(서식), 북마크, 최근 목록, 즐겨찾기, 독서 진행률, 태그 등이 JSON 파일로 `SYNC_FOLDER_DEVICE_PROFILE`에 저장되고 Google Drive와 양방향 동기화됨. |
| 동작 | `MainTabs2`의 `SwipeRefreshLayout`을 당기면(pull-to-refresh) `GFile.runSyncService()`가 호출되어 동기화를 시작. |

### 5.4 OPDS (Online Catalog)

| 구성 요소 | 설명 |
|-----------|------|
| `OPDS` | OkHttp 기반의 HTTP 클라이언트. OPDS 1.0/2.0 Atom 피드를 다운로드. 캐싱 및 프록시 설정 지원. |
| `OpdsItem` | OPDS 카탈로그 항목 하나를 표현하는 데이터 클래스. |
| `Feed` / `Entry` | Atom 피드 및 엔트리를 파싱한 결과 모델. |
| `OpdsFragment2` | OPDS 탭 UI. 사용자가 추가한 OPDS URL들을 목록으로 보여주고, 검색 및 탐색을 가능하게 함. |
| 동작 | 사용자가 OPDS URL 입력 -> `OPDS.getHttpResponse()`로 Atom/XML 다운로드 -> `XmlParser`로 파싱 -> `Feed`/`Entry` 객체로 변환 -> `OpdsFragment2`의 Adapter에 표시. |

### 5.5 Google Drive 연동

| 구성 요소 | 설명 |
|-----------|------|
| `GFile` | Google Drive REST API (`com.google.api.services.drive`)를 사용하여 파일 업로드/다운로드/삭제/목록 조회. |
| `GoogleDriveFragment2` | 클라우드 탭 UI. Google Drive에 저장된 책 파일을 직접 탐색하고 열 수 있음. |
| 인증 | `GoogleSignIn` (OAuth 2.0)을 통해 사용자 인증. `REQUEST_CODE_SIGN_IN`으로 로그인 Intent를 시작하고 `onActivityResult`에서 액세스 토큰을 획득. |

---

## 6. 핵심 클래스 다이어그램 (텍스트 기반)

### 6.1 데이터 모델 및 설정 계층

```
+----------------+     +----------------+     +----------------+
|   AppProfile   |     |    AppState    |     |    BookCSS     |
|   (싱글톤)      |     |   (싱글톤)      |     |   (싱글톤)      |
|                |     |                |     |                |
| - SYNC_FOLDER_*|     | -appTheme      |     | -fontSizeSp    |
| - profile      |     | -fullScreenMode|     | -documentStyle |
| - init()       |     | -tabsOrder9    |     | -isAutoHypens  |
| - save()       |     | -save()        |     | -get()         |
+----------------+     +----------------+     +----------------+
         |                      |                      |
         |                      |                      |
         v                      v                      v
+-----------------------------------------------------------+
|                         AppSP (SharedPreferences)           |
|  - lastBookPath, readingMode, currentProfile, isEnableSync  |
+-----------------------------------------------------------+

+----------------+     +----------------+     +----------------+
|    FileMeta    |     |   SimpleMeta   |     |   AppBookmark  |
+------|---------+     +----------------+     +----------------+
       |                                              |
       +--------------------------------------------->+
       |                                              |
       v                                              v
+----------------+     +----------------+
|   FileMetaDao  |<-->|   DaoSession   |
| (GreenDAO DAO) |     | (GreenDAO ORM) |
+----------------+     +----------------+
```

### 6.2 뷰어 및 렌더링 계층

```
+-------------------------------------------+
|          AdsFragmentActivity              |
|          (광고 기반 Activity)               |
+-------------------------------------------+
                    ^
                    |
    +---------------+---------------+
    |                               |
    v                               v
+---------------------+     +---------------------+
| HorizontalViewAct.  |     | VerticalViewAct.  |
| (수평 텍스트 뷰어)    |     | (수직 이미지 뷰어)   |
+---------------------+     +---------------------+
            |                           |
            v                           v
+-------------------------------------------+
|          DocumentController (Abstract)    |
|  - onGoToPage, onZoom, onNextPage...      |
|  - saveCurrentPage(), loadOutline()       |
+-------------------------------------------+
            |
            +----------------------------+
            |                            |
            v                            v
+---------------------+     +---------------------+
|   ImageExtractor    |     | [Format Parsers]    |
|  (캐싱 & 비트맵 관리) |     |  - libfb2           |
|                     |     |  - libmobi          |
+----------+----------+     |  - ext (EPUB/RTF)   |
           |                +----------+----------+
           |                           |
           v                           v
+-------------------------------------------+
|         MuPDF JNI (CodecDocument)         |
|         (페이지 -> Bitmap 렌더링)           |
+-------------------------------------------+
```

### 6.3 애플리케이션 전체 구조 요약

```
[Presentation Layer (UI)]
  - MainTabs2, HorizontalViewActivity, VerticalViewActivity
  - Fragment2 classes (Recent, Browse, Search, OPDS, etc.)
  - Custom Views (DrawView, TTSControlsView, SeekBar...)

[Controller Layer]
  - DocumentController (Abstract)
  - ExtUtils (파일 오픈, 포맷 분기)
  - AppsConfig (빌드 플레이버 설정)

[Service Layer]
  - TTSService (Foreground)
  - BooksService (백그라운드 색인)
  - SynctornizatoinWorker (WorkManager)

[Business Logic / Domain Layer]
  - AppData, AppProfile, AppState, BookCSS
  - OPDS, GFile, Clouds
  - TTSEngine, ImageExtractor

[Data Layer]
  - AppDB (GreenDAO 싱글톤)
  - FileMetaDao, DictMetaDao
  - SharedBooks (ebookdroid 라이브러리 설정)

[External / Native Layer]
  - MuPDF (JNI)
  - libmobi (JNI)
  - libdjvu (JNI)
```

---

## 7. 데이터 모델 상세

### 7.1 GreenDAO 데이터베이스 (`dao2`)

Librera는 ORM(Object-Relational Mapping)으로 **GreenDAO**를 사용한다.

| 클래스 | 역할 | 주요 필드 |
|--------|------|-----------|
| `FileMeta` | 서적/파일 메타데이터 엔티티 | `path`, `title`, `author`, `series`, `size`, `date`, `state`, `isStar`, `isSearchBook` 등 |
| `FileMetaDao` | `FileMeta`에 대한 DAO | `queryBuilder()`, `loadAll()`, `insertOrReplace()` |
| `DictMeta` | 사전 메타데이터 엔티티 | `path`, `name` 등 |
| `DictMetaDao` | `DictMeta`에 대한 DAO | 기본 CRUD |
| `DaoMaster` | 데이터베이스 스키마 관리 | `Schema` 버전, 초기 마이그레이션 |
| `DaoSession` | DAO 인스턴스 관리 | `getFileMetaDao()`, `getDictMetaDao()` 제공 |

- **데이터베이스 위치**: `AppProfile.init()`에서 `db-{rootPathHash}-{profile}` 이름으로 생성.
- **데이터베이스 업그레이드**: `DatabaseUpgradeHelper`가 스키마 변경 시 마이그레이션을 처리.

### 7.2 전역 설정 모델 (`model`)

#### `AppState`
- **역할**: 앱의 모든 사용자 설정을 담당하는 거대한 POJO 클래스.
- **저장 위치**: `AppProfile.SYNC_FOLDER_DEVICE_PROFILE/app-State.json` (JSON 직렬화).
- **주요 설정**:
  - `appTheme`: 테마 (라이트/다크/OLED/잉크)
  - `fullScreenMode`, `orientation`: 전체화면 및 화면 회전
  - `tabsOrder9`: 탭 노출 순서 및 가시성
  - `tintColor`: 앱 강조색
  - `supportPDF`, `supportEPUB` 등: 지원 포맷 플래그
  - `pagesInMemory`: 메모리에 캐시할 페이지 수
  - `ttsSpeed`, `ttsPitch`: TTS 속도/피치
  - `nextKeys`, `prevKeys`: 페이지 넘김 키 설정

#### `BookCSS`
- **역할**: 서적 내 텍스트 스타일(폰트, 여백, 정렬, 들여쓰기 등)을 관리.
- **저장 위치**: `app-CSS.json`.
- **주요 설정**:
  - `fontSizeSp`: 기본 폰트 크기
  - `fontFolder`: 사용자 정의 폰트 경로
  - `documentStyle`: 문서 스타일 적용 방식 (`STYLES_DOC_AND_USER`, `STYLES_ONLY_DOC`, `STYLES_ONLY_USER`)
  - `isAutoHypens`: 자동 하이픈네이션 여부
  - `marginTop/Bottom/Left/Right`: 페이지 여백
  - `textIndent`: 단락 들여쓰기

#### `AppProfile`
- **역할**: 앱 전체의 파일 시스템 경로, 프로필 관리, 동기화 루트 폴더 설정.
- **주요 책임**:
  - `init(Context)`: 루트 폴더(`Librera/`) 생성, 데이터베이스 초기화, JSON 설정 로드.
  - `save(Context)`: `AppState`, `BookCSS`, `AppSP`를 JSON으로 저장.
  - 프로필(Profile) 관리: 사용자 프로필별로 독립된 설정 및 동기화 폴더를 생성. `profile.{name}/device.{MODEL}/` 구조.

#### `AppSP` (SharedPreferences Wrapper)
- **역할**: 가벼운 키-값 저장. `lastBookPath`, `readingMode`, `isEnableSync` 등 빈번하게 읽고 써야 하는 값을 저장.

#### `AppBook` & `SharedBooks`
- **역할**: 특정 서적의 독서 진행 정보(현재 페이지, 확대/축소 레벨, 회전 상태 등)를 관리.
- **저장 방식**: `SharedBooks`가 `SharedPreferences`를 통해 서적 경로별 설정을 저장.

### 7.3 데이터 흐름 요약

```
[사용자 설정 변경]
    |
    v
[AppState / BookCSS 객체 업데이트]
    |
    v
[AppProfile.save()]
    |
    +---> [JSON 파일로 직렬화] --> (Librera/profile.{name}/device.{MODEL}/app-State.json)
    |
    +---> [AppSP.save()] --> (SharedPreferences)
    |
    v
[동기화 활성화 시] --> [GFile] --> [Google Drive 업로드]
    |
    v
[앱 재실행 시] --> [AppProfile.init()] --> [JSON 역직렬화] --> 객체 복원
```

---

## 8. 주요 외부 라이브러리 의존성

| 라이브러리 | 용도 |
|-----------|------|
| **MuPDF** | PDF/DjVu/XPS/EPUB/FB2 렌더링 엔진 (C++, JNI) |
| **ebookdroid** | 기존 EBookDroid 뷰어 프레임워크 기반 (`org.ebookdroid.*`) |
| **GreenDAO** | SQLite ORM (데이터베이스 엔티티 및 DAO 자동 생성) |
| **Glide** | 이미지 로딩 및 캐싱 |
| **EventBus** | 구성 요소 간 비동기 메시지 통신 (`MessageEvent`, `InvalidateMessage` 등) |
| **OkHttp3** | OPDS 및 네트워크 통신 |
| **jsoup** | HTML 파싱 및 처리 |
| **Glide** | 썸네일 및 페이지 이미지 비동기 로딩 |
| **Firebase / Google Play Services** | 광고(AdMob), Google Drive API, 인앱 리뷰, Analytics |
| **WorkManager** | 백그라운드 도서 색인 및 동기화 작업 |
| **libmobi** | MOBI/AZW 포맷 파싱 (C 라이브러리) |
| **djvulibre** | DjVu 포맷 렌더링 (C 라이브러리) |
| **juniversalchardet** | 문자 인코딩 자동 감지 |
| **zip4j** | ZIP 파일 압축/해제 |
| **junrar** | RAR 파일 압축 해제 |

---

## 9. 빌드 및 배포 아키텍처

### 9.1 Gradle 빌드 설정

- **Gradle Kotlin DSL**: `settings.gradle.kts`, `build.gradle.kts` 사용.
- **Version Catalog**: `libs.versions.toml` (또는 동등 메커니즘)을 통해 의존성 버전 중앙 관리.

### 9.2 ABI 분할

```kotlin
splits {
    abi {
        enable = true
        include "x86", "x86_64", "armeabi-v7a", "arm64-v8a"
        universalApk = true
    }
}
```

- x86, x86_64, armeabi-v7a, arm64-v8a 4가지 ABI별 APK를 생성하며, Universal APK도 함께 생성.

### 9.3 F-Droid vs Google Play 빌드 차이

| 기능 | F-Droid 빌드 | Google Play 빌드 |
|------|-------------|----------------|
| Google Services | ❌ 없음 | ✅ google-services.json 필요 |
| Firebase Analytics | ❌ 없음 | ✅ 포함 |
| AdMob 광고 | ❌ 없음 | ✅ 포함 (`libDepFree`) |
| Google Drive 연동 | ❌ 없음 | ✅ 포함 (`libDepPro`) |
| In-App Review | ❌ 없음 | ✅ 포함 (`libDepPro`) |
| RAR 지원 | ❌ 없음 (Stub) | ✅ 포함 (`junrar`) |

---

## 10. 요약

Librera Reader는 **MuPDF + EBookDroid 프레임워크**를 기반으로 한 하이브리드 애플리케이션이다.

- **멀티모듈**: `app`, `libDepFree`, `libDepPro`, `libReflow` 등 플레이버별로 모듈을 분리하여 F-Droid와 Google Play를 동시에 지원.
- **멀티플랫폼 렌더링**: 이미지 기반(PDF/DjVu)과 텍스트 기반(EPUB/FB2/MOBI) 포맷을 모두 지원하며, 텍스트 포맷은 내부적으로 HTML -> 이미지 변환을 거쳐 통일된 페이지 경험을 제공.
- **풍부한 기능**: TTS, 하이픈네이션, OPDS, Google Drive 동기화, 다양한 테마, 프로필 시스템 등 리더기 앱으로서의 거의 모든 기능을 갖춤.
- **설정 및 데이터**: JSON 파일 기반의 설정 동기화와 GreenDAO 기반의 로컬 데이터베이스를 혼합하여 사용.
- **아키텍처 스타일**: 전통적인 Android MVC 패턴에 가까우며, `DocumentController`라는 거대한 추상 클래스가 뷰어의 모든 상태와 동작을 총괄. EventBus를 통한 느슨한 결합이 많이 사용됨.
