@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    AUTO IMPORT UPDATER 1.16.5 -> 1.21.4
echo ========================================
echo.

set "UPDATED=0"
set "TOTAL=0"

for /r "src" %%f in (*.java) do (
    set /a TOTAL+=1
    echo Processing: %%~nxf
    
    powershell -Command "
    \$content = [System.IO.File]::ReadAllText('%%f', [System.Text.Encoding]::UTF8)
    \$original = \$content
    
    # ===== IMPORT REPLACEMENTS =====
    
    # Render system
    \$content = \$content -replace 'import com\\.mojang\\.blaze3d\\.platform\\.GlStateManager;', 'import com.mojang.blaze3d.systems.RenderSystem;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.Tessellator;', 'import com.mojang.blaze3d.vertex.Tesselator;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.BufferBuilder;', 'import com.mojang.blaze3d.vertex.BufferBuilder;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.DefaultVertexFormats;', 'import com.mojang.blaze3d.vertex.DefaultVertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.VertexFormat;', 'import com.mojang.blaze3d.vertex.VertexFormat;'
    
    # Math vectors
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3d;', 'import org.joml.Vector3d;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3f;', 'import org.joml.Vector3f;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Matrix4f;', 'import org.joml.Matrix4f;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Matrix3f;', 'import org.joml.Matrix3f;'
    
    # Text formatting
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.TextFormatting;', 'import net.minecraft.ChatFormatting;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.StringTextComponent;', 'import net.minecraft.network.chat.Component;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.TranslationTextComponent;', 'import net.minecraft.network.chat.Component;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.ITextComponent;', 'import net.minecraft.network.chat.Component;'
    
    # GUI and rendering
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.gui\\.FontRenderer;', 'import net.minecraft.client.gui.Font;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.gui\\.AbstractGui;', 'import net.minecraft.client.gui.GuiGraphics;'
    
    # ===== METHOD REPLACEMENTS =====
    
    # Tessellator -> Tesselator
    \$content = \$content -replace 'Tessellator\\.getInstance\\(\\)', 'Tesselator.getInstance()'
    
    # BufferBuilder methods
    \$content = \$content -replace '\\.getBuffer\\(\\)', '.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)'
    \$content = \$content -replace '\\.begin\\(7,\\s*DefaultVertexFormats\\.POSITION_COLOR\\)', ''
    \$content = \$content -replace '\\.begin\\(7,\\s*DefaultVertexFormats\\.POSITION_TEX_COLOR\\)', ''
    \$content = \$content -replace '\\.pos\\(', '.addVertex('
    \$content = \$content -replace '\\.endVertex\\(\\)', ''
    
    # GlStateManager -> RenderSystem
    \$content = \$content -replace 'GlStateManager\\.', 'RenderSystem.'
    
    # Text formatting usage
    \$content = \$content -replace 'TextFormatting\\.', 'ChatFormatting.'
    
    # Text components
    \$content = \$content -replace 'new StringTextComponent\\(([^)]+)\\)', 'Component.literal(\$1)'
    \$content = \$content -replace 'new TranslationTextComponent\\(([^)]+)\\)', 'Component.translatable(\$1)'
    
    # Font rendering
    \$content = \$content -replace 'fontRenderer\\.drawStringWithShadow\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', 'guiGraphics.drawString(font, \$1, \$2, \$3, \$4, true)'
    \$content = \$content -replace '\\.drawStringWithShadow\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', '.drawString(font, \$1, \$2, \$3, \$4, true)'
    
    # ScaledResolution -> Window
    \$content = \$content -replace 'ScaledResolution', 'Window'
    \$content = \$content -replace 'new ScaledResolution\\([^)]+\\)', 'Minecraft.getInstance().getWindow()'
    
    if (\$content -ne \$original) {
        [System.IO.File]::WriteAllText('%%f', \$content, [System.Text.Encoding]::UTF8)
        Write-Host '  UPDATED: %%~nxf' -ForegroundColor Green
        exit 0
    } else {
        Write-Host '  No changes: %%~nxf' -ForegroundColor Gray
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
echo ========================================
echo.

pause