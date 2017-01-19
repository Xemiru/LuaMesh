package com.github.xemiru.luamesh.test;

import com.github.xemiru.luamesh.LuaType;

@LuaType(name = "libB")
public class LibraryB extends LibraryA {

    static void println(String str) {
        System.out.println(str);
    }

    @Override
    @LuaType(name = "voidmethod")
    public void voidMethod() {
        println("LibraryB overrides void method.");
    }
}
