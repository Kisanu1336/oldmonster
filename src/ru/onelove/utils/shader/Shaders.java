package ru.onelove.utils.shader;

import ru.onelove.utils.shader.shaders.AlphaGlsl;
import ru.onelove.utils.shader.shaders.ContrastGlsl;
import ru.onelove.utils.shader.shaders.FontGlsl;
import ru.onelove.utils.shader.shaders.GaussianBloomGlsl;
import ru.onelove.utils.shader.shaders.KawaseDownGlsl;
import ru.onelove.utils.shader.shaders.KawaseUpGlsl;
import ru.onelove.utils.shader.shaders.MaskGlsl;
import ru.onelove.utils.shader.shaders.OutlineGlsl;
import ru.onelove.utils.shader.shaders.VertexGlsl;
import ru.onelove.utils.shader.shaders.WhiteGlsl;
import lombok.Getter;
import ru.onelove.utils.shader.shaders.RoundedGlsl;
import ru.onelove.utils.shader.shaders.RoundedOutGlsl;
import ru.onelove.utils.shader.shaders.SmoothGlsl;

public class Shaders {
    @Getter
    private static Shaders Instance = new Shaders();
    @Getter
    private IShader font = new FontGlsl();
    @Getter
    private IShader vertex = new VertexGlsl();
    @Getter
    private IShader rounded = new RoundedGlsl();
    @Getter
    private IShader roundedout = new RoundedOutGlsl();
    @Getter
    private IShader smooth = new SmoothGlsl();
    @Getter
    private IShader white = new WhiteGlsl();
    @Getter
    private IShader alpha = new AlphaGlsl();
    @Getter
    private IShader gaussianbloom = new GaussianBloomGlsl();
    @Getter
    private IShader kawaseUp = new KawaseUpGlsl();
    @Getter
    private IShader kawaseDown = new KawaseDownGlsl();
    @Getter
    private IShader outline = new OutlineGlsl();
    @Getter
    private IShader contrast = new ContrastGlsl();
    @Getter
    private IShader mask = new MaskGlsl();
}
