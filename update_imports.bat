@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    AUTOMATIC IMPORTS UPDATER FOR 1.21.4
echo ========================================
echo.

set "SOURCE_DIR=src"
set "BACKUP_DIR=backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%"

echo Creating backup to !BACKUP_DIR!...
xcopy "!SOURCE_DIR!" "!BACKUP_DIR!\" /E /I /H >nul

echo.
echo Updating imports in Java files...
echo.

set "UPDATED=0"
set "TOTAL=0"

for /r "!SOURCE_DIR!" %%f in (*.java) do (
    set /a TOTAL+=1
    set "FILE_UPDATED=0"
    
    echo Processing: %%~nxf
    
    powershell -Command "
    $content = Get-Content '%%f' -Raw
    \$original = \$content
    
    # Replace imports
    \$content = \$content -replace 'import com\\.mojang\\.blaze3d\\.platform\\.GlStateManager;', 'import com.mojang.blaze3d.systems.RenderSystem;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.Tessellator;', 'import com.mojang.blaze3d.vertex.Tesselator;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.BufferBuilder;', 'import com.mojang.blaze3d.vertex.BufferBuilder;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.DefaultVertexFormats;', 'import com.mojang.blaze3d.vertex.DefaultVertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.VertexFormat;', 'import com.mojang.blaze3d.vertex.VertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3d;', 'import org.joml.Vector3d;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3f;', 'import org.joml.Vector3f;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Matrix4f;', 'import org.joml.Matrix4f;'
    
    # Replace method calls
    \$content = \$content -replace 'Tessellator\\.getInstance\\(\\)', 'Tesselator.getInstance()'
    \$content = \$content -replace '\\.getBuffer\\(\\)', '.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)'
    \$content = \$content -replace '\\.begin\\([^)]+\\)', ''
    \$content = \$content -replace '\\.pos\\(', '.addVertex('
    \$content = \$content -replace '\\.endVertex\\(\\)', ''
    \$content = \$content -replace 'GlStateManager\\.', 'RenderSystem.'
    
    if (\$content -ne \$original) {
        Set-Content '%%f' \$content
        Write-Host '  -> Updated' -ForegroundColor Green
        exit 0
    } else {
        Write-Host '  -> No changes' -ForegroundColor Yellow
        exit 1
    }
    "
    
    if !errorlevel! equ 0 (
        set /a UPDATED+=1
    )
)

echo.
echo ========================================
echo SUMMARY:
echo Total files processed: !TOTAL!
echo Files updated: !UPDATED!
echo Backup created in: !BACKUP_DIR!
echo ========================================
echo.

pause