# LuaMesh

Mesh your Java objects into a Lua environment using LuaJ.

## Importing into your project

The project can be retrieved as an artifact from the [Maven Central repository.](https://search.maven.org/#artifactdetails%7Ccom.github.xemiru%7Cluamesh%7C1.0%7Cjar)

#### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
  compile 'com.github.xemiru:luamesh:1.0'
}
```

#### Maven
```xml
<dependency>
    <groupId>com.github.xemiru</groupId>
    <artifactId>luamesh</artifactId>
    <version>1.0</version>
</dependency>
```

## Usage

All documentation is available at [http://xemiru.github.io/LuaMesh/latest](http://xemiru.github.io/LuaMesh/latest).

Need them for a specific version? Replace `latest` with the version you need. All documentation pages are in the [gh-pages branch](https://github.com/Xemiru/LuaMesh/tree/gh-pages).

----

LuaMesh allows you to pass your Java objects cleanly into Lua environments, managed by LuaJ. The main classes to perform this task are `LuaMesh`, `LuaType`, and `LuaObjectValue`.

All Java objects, by default, can be passed into the Lua environment when wrapped by a `LuaObjectValue`. This however only serves to let an object be carried into Lua and back with no other functionality; the Lua object will not have Lua functions that call corresponding Java methods by default.

```java
Globals g = ...;
Object obj = ...;

g.set("javaobj", LuaObjectValue.of(obj));
```

For the passed object to have usable methods, the object's class and Lua-accessible methods must be annotated with `@LuaType`.

```java
package luamesh.is.ok.iguess

@LuaType
public class MeshedObject {

    @LuaType
    public void myMethod() {
        System.out.println("Hello, world!");
    }

}
```

Afterwards, LuaMesh needs to be told which classes are annotated with LuaType. This can be done by passing the classes' fully qualified name to the `register` method.

Once all classes are listed, the `init` method is called.

```java
public class Main {

    public static void main(String[] args) {
        LuaMesh.register("luamesh.is.ok.iguess.MeshedObject");

        // ... other classes

        LuaMesh.init();
    }

}
```

It is recommended that LuaMesh be the **FIRST** thing to be set up, as class transformations that inject the necessary code require that the target classes not be loaded by the Java Virtual Machine prior. This means references to the classes at any point must not occur before the `init` method (which means something like `LuaMesh.register(MyClass.class.getName())` would be illegal).

## LuaType annotation

[LuaType javadoc here.](https://xemiru.github.io/LuaMesh/latest/com/github/xemiru/luamesh/LuaType.html)

---

Methods to be passed as a Lua function can be set to have a different name from the Java method, by setting an overriding value to the `LuaType` annotation.

```java
@LuaType(name = "luaMethod")
public void javaMethod() {
    // now appears as "luaMethod" on the Lua object
}
```

The same can be done to the class itself, having a different name returned by the `ctype` function.

```java
@LuaType(name = "myobject")
public class MyObject {
    // ctype on Lua MyObjects now return "myobject"
}
```

If no override is passed, it will by default simply copy the declared name. This may not be favorable for some naming schemes if one wishes to follow a separate one for the Lua environment. The most common changes for naming schemes can be applied by LuaMesh directly, and can be configured by changing constants within the `LuaMesh` class.

---

Lua allows objects to override their behavior when passed to an operator by pushing a corresponding function to their metatables. This can be done by LuaMesh by passing the metafunction to overwrite to the `LuaType` annotation.

```java
@LuaType(entry = MetaEntry.ADD)
public MyObject madd(MyObject obj) {
    // MyObject + MyObject in lua now calls this method
}
```

## Version history

| Version | Notes |
|:-:|:--|
| 1.0     | Initial release. |
