@echo off
chcp 65001 >nul
echo ============================================================================
echo [SHINSEGAE FOOD FE] GitHub 자동 APK 빌드 및 무인 업데이트 저장소 원클릭 구성기
echo ============================================================================

where gh >nul 2>nul
if %errorlevel% neq 0 (
    echo [오류] GitHub CLI ^(gh^)가 설치되어 있지 않습니다. https://cli.github.com 에서 설치해주세요.
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('gh api user -q ".login"') do set GH_USER=%%i
if "%GH_USER%"=="" (
    echo [오류] GitHub CLI 로그인 상태를 확인할 수 없습니다. 'gh auth login'을 먼저 실행해주세요.
    pause
    exit /b 1
)

set REPO_NAME=shinsegae-fe-cad
echo [확인 완료] 로그인된 GitHub 계정: %GH_USER%
echo [저장소 경로] https://github.com/%GH_USER%/%REPO_NAME%

REM 1. 고정 서명키(release-key.jks)가 없으면 최초 1회 자동 생성
if not exist "release-key.jks" (
    echo [1/4] 고정 안드로이드 서명키^(release-key.jks^) 최초 1회 생성 중...
    keytool -genkeypair -v -keystore release-key.jks -alias fecadkey -keyalg RSA -keysize 2048 -validity 10000 -storepass ssgfe2026! -keypass ssgfe2026! -dname "CN=ShinsegaeFoodFE, OU=FE, O=ShinsegaeFood, L=Osan, S=Gyeonggi, C=KR"
)

REM 2. GitHub 저장소 생성 (이미 있으면 건너뜀)
echo [2/4] GitHub 저장소 생성 및 연결 중...
gh repo view %GH_USER%/%REPO_NAME% >nul 2>nul
if %errorlevel% neq 0 (
    gh repo create %REPO_NAME% --public --description "SHINSEGAE FOOD FE 3D SketchUp & DWG CAD Auto-Updating Android App"
)

REM 3. 고정 서명키를 GitHub Actions Secrets에 자동 등록
echo [3/4] 고정 서명키를 GitHub Secrets에 등록 중...
certutil -encode release-key.jks keystore_b64.txt >nul
findstr /v /c:"---" keystore_b64.txt > keystore_clean.b64
gh secret set KEYSTORE_BASE64 -R %GH_USER%/%REPO_NAME% < keystore_clean.b64
gh secret set KEYSTORE_PASSWORD -R %GH_USER%/%REPO_NAME% --body "ssgfe2026!"
gh secret set KEY_ALIAS -R %GH_USER%/%REPO_NAME% --body "fecadkey"
gh secret set KEY_PASSWORD -R %GH_USER%/%REPO_NAME% --body "ssgfe2026!"
del keystore_b64.txt keystore_clean.b64

REM 4. Git 초기화, 커밋 및 첫 릴리스 태그(v1.2.0) 푸시 -> GitHub Actions가 APK 자동 빌드!
echo [4/4] 소스코드 및 v1.2.0 릴리스 태그 푸시 중...
git init
git branch -M main
git remote remove origin 2>nul
git remote add origin https://github.com/%GH_USER%/%REPO_NAME%.git
git add .
git commit -m "feat: initial auto-updating APK release v1.2.0"
git tag -f v1.2.0
git push -u origin main --force
git push origin v1.2.0 --force

echo.
echo ============================================================================
echo [완료!] GitHub Actions에서 APK 빌드가 시작되었습니다! (약 2분 소요)
echo 첫 설치용 APK 다운로드 링크:
echo https://github.com/%GH_USER%/%REPO_NAME%/releases/latest/download/shinsegae-fe-cad-latest.apk
echo ============================================================================
pause
