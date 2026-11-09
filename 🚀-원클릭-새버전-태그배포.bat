@echo off
chcp 65001 >nul
echo ============================================================================
echo [SHINSEGAE FOOD FE] 새 버전 APK 배포기 (GitHub Actions 트리거)
echo ============================================================================
set /p NEW_TAG="배포할 새 버전 태그를 입력하세요 (예: v1.2.1): "
if "%NEW_TAG%"=="" (
    echo [오류] 태그명이 입력되지 않았습니다.
    pause
    exit /b 1
)

set /p COMMIT_MSG="커밋 메시지를 입력하세요 (기본: feat: new release %NEW_TAG%): "
if "%COMMIT_MSG%"=="" set COMMIT_MSG=feat: new release %NEW_TAG%

echo.
echo [1/3] 최신 HTML을 안드로이드 assets에 동기화 중...
copy /Y shinsegae_fe_sketchup_studio.html android\app\src\main\assets\index.html >nul

echo [2/3] Git 커밋 및 태그 생성 중...
git add .
git commit -m "%COMMIT_MSG%"
git tag %NEW_TAG%

echo [3/3] GitHub로 푸시 중 (GitHub Actions에서 APK가 자동 빌드됩니다)...
git push origin main
git push origin %NEW_TAG%

echo.
echo ============================================================================
echo [완료] GitHub Actions 빌드가 시작되었습니다. 약 2분 뒤 아래 링크에서 다운로드 가능합니다:
echo https://github.com/jud19876-cell/shinsegae-fe-cad/releases/latest/download/shinsegae-fe-cad-latest.apk
echo ============================================================================
pause
