package ru.onelove.scripts.interpreter.lib;

import ru.onelove.scripts.interpreter.LuaValue;

class TableLibFunction extends LibFunction {
	public LuaValue call() {
		return argerror(1, "table expected, got no value");
	}
}
