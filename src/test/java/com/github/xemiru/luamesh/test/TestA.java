package com.github.xemiru.luamesh.test;

import com.github.xemiru.luamesh.LuaMesh;
import com.github.xemiru.luamesh.LuaType;
import com.github.xemiru.luamesh.LuaType.MetaEntry;

@LuaType
public class TestA {

    public static void println(String msg) {
        System.out.println(msg);
    }

    // =========================================
    // method test

    public void fuck(Object obj) {
        LuaMesh.lua(this, "fuck", () -> {
            System.out.println(obj);
        }, obj);
    }

    @LuaType
    public int intMethod() {
        println("Integer method returns 0.");
        return 0;
    }

    @LuaType
    public void voidMethod() {
        println("Void method does nothing.");
    }

    @LuaType
    public Object objMethod(Object obj) {
        return obj;
    }

    // =========================================
    // type annot tests

    @LuaType(abstractt = true)
    public boolean abstractMethod() {
        println("This shouldn't be printed out.");
        return true;
    }

    @LuaType(entry = MetaEntry.ADD)
    public TestA add(TestA b) {
        println("Addition method returns the second object.");
        return b;
    }

    public void notInLua() {
        println("This shouldn't be printed out from Lua.");
    }

}
