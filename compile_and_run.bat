@echo off
chcp 65001 >nul

echo ========================================
echo    COMPILE AND RUN SCRIPT FOR 1.21.4
echo ========================================
echo.

echo Step 1: Creating list of Java files...
dir /s /b src\*.java > java_files.txt

echo Step 2: Compiling Java files...
javac -cp "libs/*" -d out/production/client @java_files.txt

if %errorlevel% neq 0 (
    echo.
    echo COMPILATION FAILED!
    echo Check the errors above.
    pause
    exit /b 1
)

echo.
echo Step 3: Running Minecraft 1.21.4...
java -cp "out/production/client;libs/*" -Djava.library.path=libs Start

echo.
pause