@echo off
chcp 65001 >nul

echo Quick fix for Tessellator and imports...
echo.

for /r "src" %%f in (*.java) do (
    powershell -Command "
    \$content = [System.IO.File]::ReadAllText('%%f')
    \$old = \$content
    
    # Only critical replacements
    \$content = \$content -replace 'Tessellator', 'Tesselator'
    \$content = \$content -replace 'import com\\.mojang\\.blaze3d\\.platform\\.GlStateManager', 'import com.mojang.blaze3d.systems.RenderSystem'
    \$content = \$content -replace 'DefaultVertexFormats', 'DefaultVertexFormat'
    
    if (\$content -ne \$old) {
        [System.IO.File]::WriteAllText('%%f', \$content, [System.Text.Encoding]::UTF8)
        Write-Host 'Fixed: %%~nxf'
    }
    "
)

echo Done!
pause