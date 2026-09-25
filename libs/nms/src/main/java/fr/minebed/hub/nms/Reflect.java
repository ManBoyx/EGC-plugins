package fr.minebed.hub.nms;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Petits outils de réflexion qui ne lèvent jamais d'exception : « introuvable » se dit {@code null}. */
public final class Reflect {

    private Reflect() {
    }

    public static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    public static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    public static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /** Appelle une méthode ; renvoie {@code null} si l'appel échoue (à distinguer d'un résultat nul par {@link #ok}). */
    public static Object invoke(Method method, Object target, Object... arguments) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
