@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    MANUAL IMPORTS FIXER FOR 1.21.4
echo ========================================
echo.

set "count=0"

for /r "src" %%f in (*.java) do (
    echo Processing: %%~nxf
    
    powershell -Command "
    \$content = [System.IO.File]::ReadAllText('%%f')
    \$original = \$content
    
    # Import replacements
    \$content = \$content -replace 'import com\\.mojang\\.blaze3d\\.platform\\.GlStateManager;', 'import com.mojang.blaze3d.systems.RenderSystem;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.Tessellator;', 'import com.mojang.blaze3d.vertex.Tesselator;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.BufferBuilder;', 'import com.mojang.blaze3d.vertex.BufferBuilder;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.DefaultVertexFormats;', 'import com.mojang.blaze3d.vertex.DefaultVertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.VertexFormat;', 'import com.mojang.blaze3d.vertex.VertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3d;', 'import org.joml.Vector3d;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3f;', 'import org.joml.Vector3f;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Matrix4f;', 'import org.joml.Matrix4f;'
    
    # Method replacements
    \$content = \$content -replace 'Tessellator\\.getInstance\\(\\)', 'Tesselator.getInstance()'
    \$content = \$content -replace 'BufferBuilder\\s+\\w+\\s*=\\s*\\w+\\.getBuffer\\(\\)', '\$0.replace('getBuffer()', 'begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)')'
    \$content = \$content -replace '\\.begin\\(7,\\s*DefaultVertexFormats\\.POSITION_COLOR\\)', ''
    \$content = \$content -replace '\\.pos\\(', '.addVertex('
    \$content = \$content -replace '\\.endVertex\\(\\)', ''
    \$content = \$content -replace 'GlStateManager\\.', 'RenderSystem.'
    
    if (\$content -ne \$original) {
        [System.IO.File]::WriteAllText('%%f', \$content, [System.Text.Encoding]::UTF8)
        Write-Host '  UPDATED: %%~nxf' -ForegroundColor Green
    } else {
        Write-Host '  No changes: %%~nxf' -ForegroundColor Gray
    }
    "
    
    set /a count+=1
)

echo.
echo Processed !count! files.
echo ========================================
pause