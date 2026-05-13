@echo off
setlocal enabledelayedexpansion

set MAVEN_VERSION=3.9.9
set MAVEN_DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_USER_HOME=%USERPROFILE%\.m2
set MAVEN_HOME=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%\apache-maven-%MAVEN_VERSION%
set MAVEN_EXE=%MAVEN_HOME%\bin\mvn.cmd

if exist "%MAVEN_EXE%" goto RunMaven

echo Downloading Apache Maven %MAVEN_VERSION%...
if not exist "%MAVEN_HOME%" (
    mkdir "%MAVEN_HOME%" 2>nul || true
)

set DIST_DIR=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

set TMP_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip
powershell -Command "Invoke-WebRequest '%MAVEN_DIST_URL%' -OutFile '%TMP_ZIP%'"
powershell -Command "Expand-Archive -Path '%TMP_ZIP%' -DestinationPath '%DIST_DIR%' -Force"
del /Q "%TMP_ZIP%"
echo Maven %MAVEN_VERSION% installed.

:RunMaven
"%MAVEN_EXE%" %*
endlocal
