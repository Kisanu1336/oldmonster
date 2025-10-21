package ru.onelove.scripts.interpreter.globals;

import ru.onelove.scripts.interpreter.compiler.LuaC;
import ru.onelove.scripts.interpreter.Globals;
import ru.onelove.scripts.interpreter.LoadState;
import ru.onelove.scripts.interpreter.lib.*;
import ru.onelove.scripts.lua.libraries.ModuleLibrary;
import ru.onelove.scripts.lua.libraries.PlayerLibrary;

public class Standarts {
    public static Globals standardGlobals() {
        Globals globals = new Globals();
        globals.load(new BaseLib());
        globals.load(new Bit32Lib());
        globals.load(new MathLib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new PlayerLibrary());
        globals.load(new ModuleLibrary());
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }
}
