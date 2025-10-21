@echo off
chcp 65001 >nul

echo ========================================
echo    CHECKING FOR REMAINING OLD IMPORTS
echo ========================================
echo.

set "ISSUES=0"

echo Checking for old imports...
for /r "src" %%f in (*.java) do (
    findstr /i "Tessellator GlStateManager DefaultVertexFormats TextFormatting StringTextComponent" "%%f" >nul
    if !errorlevel! equ 0 (
        echo FOUND OLD: %%f
        set /a ISSUES+=1
    )
)

echo.
if !ISSUES! equ 0 (
    echo ✓ No old imports found!
) else (
    echo Found !ISSUES! files with old imports.
    echo Run update_imports_full.bat again.
)

echo.
pause