package fr.minebed.hub.compat;

import fr.minebed.hub.nms.Reflect;
import java.lang.reflect.Method;

/** Accès par réflexion à ce qui n'a pas la même forme d'une version à l'autre (énumérations devenues interfaces, etc.). */
final class Reflection {

    private Reflection() {
    }

    /** Appelle une méthode statique sur une classe, même si cette classe est devenue une interface. */
    static Object callStatic(String className, String method, Class<?>[] types, Object... args) {
        Class<?> type = Reflect.findClass(className);
        Method m = Reflect.findMethod(type, method, types);
        return Reflect.invoke(m, null, args);
    }
}
