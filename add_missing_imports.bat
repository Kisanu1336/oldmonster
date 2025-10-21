@echo off
chcp 65001 >nul

echo ========================================
echo    ADDING MISSING IMPORTS
echo ========================================
echo.

for /r "src" %%f in (*.java) do (
    powershell -Command "
    \$content = [System.IO.File]::ReadAllText('%%f', [System.Text.Encoding]::UTF8)
    
    # Add imports if they are used but not imported
    if (\$content -match 'ChatFormatting\\.' -and \$content -notmatch 'import net\\.minecraft\\.ChatFormatting') {
        \$content = \$content -replace 'import', 'import net.minecraft.ChatFormatting;\$nimport'
    }
    
    if (\$content -match 'Component\\.' -and \$content -notmatch 'import net\\.minecraft\\.network\\.chat\\.Component') {
        \$content = \$content -replace 'import', 'import net.minecraft.network.chat.Component;\$nimport'
    }
    
    if (\$content -match 'Tesselator\\.' -and \$content -notmatch 'import com\\.mojang\\.blaze3d\\.vertex\\.Tesselator') {
        \$content = \$content -replace 'import', 'import com.mojang.blaze3d.vertex.Tesselator;\$nimport'
    }
    
    [System.IO.File]::WriteAllText('%%f', \$content, [System.Text.Encoding]::UTF8)
    "
)

echo Missing imports added!
pause