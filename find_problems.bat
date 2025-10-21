@echo off
chcp 65001 >nul

echo ========================================
echo    FIND PROBLEMATIC IMPORTS
echo ========================================
echo.

echo Searching for old imports...
echo.

set "FOUND=0"

for /r src %%f in (*.java) do (
    findstr /i "Tessellator GlStateManager DefaultVertexFormats Vector3d Vector3f Matrix4f" "%%f" >nul
    if !errorlevel! equ 0 (
        echo Found in: %%f
        set /a FOUND+=1
    )
)

echo.
if !FOUND! equ 0 (
    echo No problematic imports found! ✓
) else (
    echo Found !FOUND! files with old imports.
    echo Run update_imports.bat to fix them.
)

echo.
pause