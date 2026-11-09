# CLAUDE.md — SHINSEGAE FE CAD & Layout Studio 자동 APK 배포 지침

이 저장소의 모든 AI 에이전트(Claude Code, Antigravity 등) 세션은 아래 APK 서명 및 GitHub Actions 자동 업데이트 배포 규칙을 반드시 따른다.

## 1. 고정 서명키(Signing Key) 규칙
- 안드로이드 앱 덮어쓰기 업데이트(`PackageInstaller`)가 작동하려면 **모든 빌드가 동일한 서명키(`release-key.jks`)**로 서명되어야 한다.
- 최초 1회 생성된 `release-key.jks`는 절대 삭제하거나 재생성하지 않으며, GitHub Secrets(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)에 고정 등록해 사용한다.

## 2. GitHub Actions 자동 빌드 & versionCode 자동 증가
- 새 버전을 배포할 때는 `vX.Y.Z` 형식의 Git 태그를 푸시한다:
  ```bash
  cp shinsegae_fe_sketchup_studio.html android/app/src/main/assets/index.html
  git add .
  git commit -m "release: v1.2.1 업데이트"
  git tag v1.2.1
  git push origin main --tags
  ```
- `.github/workflows/release-apk.yml` 워크플로우가 태그 푸시를 감지하여 `versionCode = ${{ github.run_number }}`로 매번 자동 증가시키고, 서명된 APK를 빌드해 GitHub Releases에 자동 첨부한다.

## 3. 앱 내 자동 업데이트 확인 및 무인 설치 조건
1. 앱을 열 때마다, 그리고 **15분마다** GitHub Releases API(`/repos/<owner>/<repo>/releases/latest`)를 확인한다.
2. 현재 버전보다 높은 버전이 있으면 APK를 백그라운드로 미리 다운로드(`prefetchUpdate`)해 두고, 상단 배너로 안내하거나 **사용자가 앱에서 나가는 순간(`onStop`) `PackageInstaller`로 설치**한다.
3. `PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED` 및 `UPDATE_PACKAGES_WITHOUT_USER_ACTION` 권한을 사용하여 안드로이드 12(API 31) 이상에서는 첫 1회 설치 이후 확인창 없이 업데이트되게 한다.
4. 사용자가 화면을 보고 작업 중인 동안에는 강제로 설치 창을 띄워 작업을 끊지 않는다.
