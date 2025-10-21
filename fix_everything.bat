@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo    COMPLETE 1.16.5 -> 1.21.4 FIXER
echo ========================================
echo.

set "UPDATED=0"

for /r "src" %%f in (*.java) do (
    echo Processing: %%~nxf
    
    powershell -Command "
    \$content = [System.IO.File]::ReadAllText('%%f', [System.Text.Encoding]::UTF8)
    \$original = \$content
    
    # ========== IMPORTS ==========
    \$content = \$content -replace 'import com\\.mojang\\.blaze3d\\.platform\\.GlStateManager;', 'import com.mojang.blaze3d.systems.RenderSystem;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.Tessellator;', 'import com.mojang.blaze3d.vertex.Tesselator;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.BufferBuilder;', 'import com.mojang.blaze3d.vertex.BufferBuilder;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.DefaultVertexFormats;', 'import com.mojang.blaze3d.vertex.DefaultVertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.renderer\\.vertex\\.VertexFormat;', 'import com.mojang.blaze3d.vertex.VertexFormat;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3d;', 'import org.joml.Vector3d;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Vector3f;', 'import org.joml.Vector3f;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.math\\.vector\\.Matrix4f;', 'import org.joml.Matrix4f;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.TextFormatting;', 'import net.minecraft.ChatFormatting;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.StringTextComponent;', 'import net.minecraft.network.chat.Component;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.TranslationTextComponent;', 'import net.minecraft.network.chat.Component;'
    \$content = \$content -replace 'import net\\.minecraft\\.util\\.text\\.ITextComponent;', 'import net.minecraft.network.chat.Component;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.gui\\.FontRenderer;', 'import net.minecraft.client.gui.Font;'
    \$content = \$content -replace 'import net\\.minecraft\\.client\\.gui\\.AbstractGui;', 'import net.minecraft.client.gui.GuiGraphics;'
    
    # ========== RENDER SYSTEM ==========
    \$content = \$content -replace 'Tessellator\\.getInstance\\(\\)', 'Tesselator.getInstance()'
    \$content = \$content -replace '\\.getBuffer\\(\\)', '.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)'
    \$content = \$content -replace '\\.begin\\(7,\\s*DefaultVertexFormats\\.POSITION_COLOR\\)', ''
    \$content = \$content -replace '\\.begin\\(7,\\s*DefaultVertexFormats\\.POSITION_TEX_COLOR\\)', ''
    \$content = \$content -replace '\\.pos\\(', '.addVertex('
    \$content = \$content -replace '\\.endVertex\\(\\)', ''
    \$content = \$content -replace 'GlStateManager\\.', 'RenderSystem.'
    
    # ========== TEXT & CHAT ==========
    \$content = \$content -replace 'TextFormatting\\.', 'ChatFormatting.'
    \$content = \$content -replace 'new StringTextComponent\\(([^)]+)\\)', 'Component.literal(\$1)'
    \$content = \$content -replace 'new TranslationTextComponent\\(([^)]+)\\)', 'Component.translatable(\$1)'
    
    # ========== FONT RENDERING ==========
    \$content = \$content -replace 'fontRenderer\\.drawStringWithShadow\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', 'guiGraphics.drawString(font, \$1, (int)\$2, (int)\$3, \$4, true)'
    \$content = \$content -replace '\\.drawStringWithShadow\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', '.drawString(font, \$1, (int)\$2, (int)\$3, \$4, true)'
    \$content = \$content -replace '\\.drawString\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', '.drawString(font, \$1, (int)\$2, (int)\$3, \$4, false)'
    \$content = \$content -replace '\\.drawCenteredString\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', '.drawString(font, \$1, (int)(\$2 - font.width(\$1)/2), (int)\$3, \$4, false)'
    \$content = \$content -replace '\\.drawCenteredStringWithShadow\\(([^,]+),([^,]+),([^,]+),([^)]+)\\)', '.drawString(font, \$1, (int)(\$2 - font.width(\$1)/2), (int)\$3, \$4, true)'
    
    # ========== MATH & VECTORS ==========
    \$content = \$content -replace 'Vector3d\\.ZERO', 'new Vector3d(0, 0, 0)'
    \$content = \$content -replace '\\.xCoord', '.x'
    \$content = \$content -replace '\\.yCoord', '.y'
    \$content = \$content -replace '\\.zCoord', '.z'
    
    # ========== SCALED RESOLUTION ==========
    \$content = \$content -replace 'ScaledResolution', 'Window'
    \$content = \$content -replace 'new ScaledResolution\\([^)]+\\)', 'Minecraft.getInstance().getWindow()'
    \$content = \$content -replace '\\.getScaledWidth\\(\\)', '.getGuiScaledWidth()'
    \$content = \$content -replace '\\.getScaledHeight\\(\\)', '.getGuiScaledHeight()'
    
    # ========== ENTITY & PLAYER ==========
    \$content = \$content -replace '\\.getPositionVec\\(\\)', '.position()'
    \$content = \$content -replace '\\.getMotion\\(\\)', '.getDeltaMovement()'
    \$content = \$content -replace '\\.setMotion\\(([^)]+)\\)', '.setDeltaMovement(\$1)'
    \$content = \$content -replace '\\.getEyeHeight\\(\\)', '.getEyeHeight()'
    \$content = \$content -replace '\\.getLookVec\\(\\)', '.getViewVector(1.0F)'
    \$content = \$content -replace '\\.rotationYaw', '.getYRot()'
    \$content = \$content -replace '\\.rotationPitch', '.getXRot()'
    \$content = \$content -replace '\\.prevRotationYaw', '.yRotO'
    \$content = \$content -replace '\\.prevRotationPitch', '.xRotO'
    \$content = \$content -replace '\\.setRotationYaw\\(([^)]+)\\)', '.setYRot(\$1)'
    \$content = \$content -replace '\\.setRotationPitch\\(([^)]+)\\)', '.setXRot(\$1)'
    
    # ========== WORLD ==========
    \$content = \$content -replace '\\.getEntitiesInAABBexcluding\\(([^,]+),([^,]+),([^)]+)\\)', '.getEntities(\$1, \$2, \$3)'
    \$content = \$content -replace '\\.getEntitiesWithinAABB\\(([^,]+),([^)]+)\\)', '.getEntities(\$1, \$2)'
    
    # ========== KEYBOARD INPUT ==========
    \$content = \$content -replace 'Keyboard\\.isKeyDown\\(([^)]+)\\)', 'InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), \$1)'
    \$content = \$content -replace 'GameSettings\\.', 'Options.'
    
    # ========== SOUND ==========
    \$content = \$content -replace '\\.playSound\\(([^,]+),([^,]+),([^,]+),([^,]+),([^,]+),([^)]+)\\)', '.playSound(\$1, \$2, \$3, \$4, \$5, \$6)'
    
    # ========== NETWORK ==========
    \$content = \$content -replace 'CPacketChatMessage', 'ServerboundChatPacket'
    \$content = \$content -replace 'new CPacketChatMessage\\(([^)]+)\\)', 'new ServerboundChatPacket(\$1)'
    
    # ========== RAY TRACE ==========
    \$content = \$content -replace '\\.rayTraceBlocks\\(([^,]+),([^)]+)\\)', '.clip(\$1, \$2)'
    
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
echo Updated !UPDATED! files
echo ========================================
pause