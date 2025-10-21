@echo off
chcp 65001 >nul

echo Checking for old imports...
echo.

set "problems=0"

for /r "src" %%f in (*.java) do (
    findstr /i "Tessellator GlStateManager DefaultVertexFormats" "%%f" >nul
    if !errorlevel! equ 0 (
        echo PROBLEM: %%f
        set /a problems+=1
    )
)

echo.
if !problems! equ 0 (
    echo ✓ No problems found!
) else (
    echo Found !problems! files with old imports.
    echo Run quick_fix.bat to fix them.
)

pause