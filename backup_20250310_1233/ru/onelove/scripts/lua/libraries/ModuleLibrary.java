package ru.onelove.scripts.lua.libraries;

import ru.onelove.scripts.interpreter.LuaValue;
import ru.onelove.scripts.interpreter.compiler.jse.CoerceJavaToLua;
import ru.onelove.scripts.interpreter.lib.OneArgFunction;
import ru.onelove.scripts.interpreter.lib.TwoArgFunction;
import ru.onelove.scripts.lua.classes.ModuleClass;

public class ModuleLibrary extends TwoArgFunction {

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        library.set("register", new register());

        env.set("module", library);
        return library;
    }

    public class register extends OneArgFunction {

        @Override
        public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(new ModuleClass(arg.toString()));
        }

    }

}
