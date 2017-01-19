package com.github.xemiru.luamesh.test;

import com.github.xemiru.luamesh.LuaLibrary;
import com.github.xemiru.luamesh.LuaType;

@LuaType
public class LibraryA extends LuaLibrary {

    static void println(String str) {
        System.out.println(str);
    }

    @LuaType
    public void voidMethod() {
        println("Library method is void.");
    }

    @LuaType
    public int intMethod() {
        println("Library method returns 3.");
        return 3;
    }

}
