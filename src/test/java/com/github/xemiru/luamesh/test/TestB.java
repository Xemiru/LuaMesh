package com.github.xemiru.luamesh.test;

import com.github.xemiru.luamesh.LuaType;

@LuaType(name = "TestBB")
public class TestB extends TestA {

    public static void println(String msg) {
        System.out.println(msg);
    }

    // test method name changes
    // because edge cases, i guess

    @Override
    @LuaType(name = "luaintMethod")
    public int intMethod() {
        println("Overriden int method returns 1.");
        return 1;
    }

}
