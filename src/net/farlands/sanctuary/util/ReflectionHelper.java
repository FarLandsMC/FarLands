package net.farlands.sanctuary.util;

import com.google.common.collect.ImmutableMap;

import com.kicas.rp.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public final class ReflectionHelper {
    private static final Map<Class<?>, Class<?>> W2P = (new ImmutableMap.Builder<Class<?>, Class<?>>())
            .put(Integer.class, int.class)
            .put(Long.class, long.class)
            .put(Short.class, short.class)
            .put(Double.class, double.class)
            .put(Float.class, float.class)
            .put(Boolean.class, boolean.class)
            .put(Byte.class, byte.class)
            .put(Character.class, char.class)
            .put(Integer[].class, int[].class)
            .put(Long[].class, long[].class)
            .put(Short[].class, short[].class)
            .put(Double[].class, double[].class)
            .put(Float[].class, float[].class)
            .put(Boolean[].class, boolean[].class)
            .put(Byte[].class, byte[].class)
            .put(Character[].class, char[].class)
            .build();

    private ReflectionHelper() { }

    public static Number numericCast(Class<?> newType, Number target) {
        if(target.getClass().equals(newType))
            return target;
        if(Integer.class.equals(newType))
            return target.intValue();
        else if(Double.class.equals(newType))
            return target.doubleValue();
        else if(Long.class.equals(newType))
            return target.longValue();
        else if(Short.class.equals(newType))
            return target.shortValue();
        else if(Float.class.equals(newType))
            return target.floatValue();
        else
            return target.byteValue();
    }

    public static <T> T safeCast(Class<T> newType, Object target) {
        if(newType.isAssignableFrom(target.getClass()))
            return newType.cast(target);
        else
            return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayCast(T[] dest, Object[] target) {
        if(dest.length != target.length)
            return null;
        for(int i = 0;i < dest.length;++ i)
            dest[i] = safeCast((Class<T>)dest.getClass().getComponentType(), target[i]);
        return dest;
    }

    public static Object[] union(Object[] a, Object[] b) {
        Set<Object> temp = new HashSet<>();
        temp.addAll(Arrays.asList(a));
        temp.addAll(Arrays.asList(b));
        temp.remove(null); // Remove the single null element if there is one
        return temp.toArray();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass, Object target) {
        Method gdecla = getMethod("getDeclaredAnnotation", target.getClass(), Class.class),
                ga = getMethod("getAnnotation", target.getClass(), Class.class);
        if(gdecla == null || ga == null)
            return null;
        T a = (T)invoke(ga, target, annotationClass);
        return a == null ? (T)invoke(gdecla, target, annotationClass) : a;
    }

    public static Method getMethod(String methodName, Class<?> clazz, Class<?>... parameterTypes) {
        return getByParameterTypesAndName(methodName, getMethods(clazz), parameterTypes);
    }

    public static Method[] getMethods(Class<?> clazz) {
        Object[] methods = union(clazz.getMethods(), clazz.getDeclaredMethods());
        return arrayCast(new Method[methods.length], methods);
    }

    public static <T extends Annotation> List<Pair<Method, T>> getMethodsAnnotatedWith(Class<?> type, Class<T> clazz) {
        List<Pair<Method, T>> result = new ArrayList<>();
        Method[] methods = getMethods(type);
        for(Method m : methods) {
            if(m.isAnnotationPresent(clazz))
                result.add(new Pair<>(m, m.getAnnotation(clazz)));
        }
        return result;
    }

    public static <T extends Annotation> Pair<Method, T> getAnnotatedMethod(Class<?> type, Class<T> clazz) {
        Method[] methods = getMethods(type);
        for(Method m : methods) {
            if(m.isAnnotationPresent(clazz))
                return new Pair<>(m, m.getDeclaredAnnotation(clazz));
        }
        return null;
    }

    public static Class<?> asWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ? FLUtils.getKey(W2P, clazz) : clazz;
    }

    public static Class<?> asNative(Class<?> clazz) {
        if(clazz.isPrimitive() || String.class.equals(clazz))
            return clazz;
        Class<?> nativeClass = W2P.get(clazz);
        return nativeClass == null ? clazz : nativeClass;
    }

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        return getByParameterTypes(getConstructors(clazz), parameterTypes);
    }

    public static Constructor<?>[] getConstructors(Class<?> clazz) {
        Object[] constructors = union(clazz.getConstructors(), clazz.getDeclaredConstructors());
        return arrayCast(new Constructor<?>[constructors.length], constructors);
    }

    @SuppressWarnings("unchecked")
    public static <T> T instantiate(Class<T> clazz, Object... parameters) {
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for(int i = 0;i < parameters.length;++ i) parameterTypes[i] = parameters[i].getClass();
        try {
            Constructor c = getConstructor(clazz, parameterTypes);
            if(c == null)
                return null;
            c.setAccessible(true);
            return (T)c.newInstance(matchParameterTypes(parameters, c.getParameterTypes()));
        }catch(InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static Object invoke(String methodName, Class<?> clazz, Object target, Object... parameters) {
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for(int i = 0;i < parameters.length;++ i) parameterTypes[i] = parameters[i].getClass();
        try {
            Method m = getMethod(methodName, clazz, parameterTypes);
            if(m == null)
                return null;
            m.setAccessible(true);
            return m.invoke(target, matchParameterTypes(parameters, m.getParameterTypes()));
        }catch(IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static Object invoke(Method method, Object target, Object... parameters) {
        try {
            method.setAccessible(true);
            return method.invoke(target, matchParameterTypes(parameters, method.getParameterTypes()));
        }catch(IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }


    public static Field getFieldObject(String fieldName, Class<?> clazz) {
        try {
            return clazz.getField(fieldName);
        }catch(NoSuchFieldException ex) {
            try {
                return clazz.getDeclaredField(fieldName);
            }catch(NoSuchFieldException ex0) {
                return null;
            }
        }
    }

    public static Field[] getFields(Class<?> clazz) {
        Object[] fields = union(clazz.getFields(), clazz.getDeclaredFields());
        return arrayCast(new Field[fields.length], fields);
    }

    public static Object getFieldValue(String fieldName, Class<?> clazz, Object target) {
        return getFieldValue(getFieldObject(fieldName, clazz), target);
    }

    public static Object getFieldValue(Field field, Object target) {
        field.setAccessible(true);
        Object result;
        try {
            result = field.get(target);
        }catch(IllegalAccessException ex) {
            return null;
        }
        return result;
    }

    public static void setFieldValue(String fieldName, Class<?> clazz, Object target, Object value) {
        setFieldValue(getFieldObject(fieldName, clazz), target, value);
    }

    public static boolean setFieldValue(Field field, Object target, Object value) {
        field.setAccessible(true);
        int mod = field.getModifiers();
        setNonFinalFieldValue(getFieldObject("modifiers", Field.class), field, mod & ~Modifier.FINAL);
        try {
            field.set(target, value);
        }catch(IllegalAccessException ex) {
            return false;
        }
        setNonFinalFieldValue(getFieldObject("modifiers", Field.class), field, mod);
        return true;
    }

    public static boolean setNonFinalFieldValue(Field field, Object target, Object value) {
        field.setAccessible(true);
        try {
            field.set(target, value);
        }catch(IllegalAccessException ex) {
            return false;
        }
        return true;
    }

    private static <T extends Executable> T getByParameterTypesAndName(String name, T[] options, Class<?>[] parameterTypes) {
        outer:
        for(T option : options) {
            if(!Objects.equals(option.getName(), name))
                continue;
            Class<?>[] opts = option.getParameterTypes(); // OPTS = option parameter types
            if(opts.length == parameterTypes.length) {
                for(int i = 0;i < opts.length;++ i) {
                    if(!opts[i].isAssignableFrom(parameterTypes[i]))
                        continue outer;
                }
            }else
                continue;
            return option;
        }
        return null;
    }

    private static <T extends Executable> T getByParameterTypes(T[] options, Class<?>[] parameterTypes) {
        outer:
        for(T option : options) {
            Class<?>[] opts = option.getParameterTypes(); // OPTS = option parameter types
            if(opts.length == parameterTypes.length) {
                for(int i = 0;i < opts.length;++ i) {
                    if(!opts[i].isAssignableFrom(parameterTypes[i]))
                        continue outer;
                }
            }else
                continue;
            return option;
        }
        return null;
    }

    private static Object[] matchParameterTypes(Object[] parameters, Class<?>[] parameterTypes) {
        Object[] matchedParams = new Object[parameters.length];
        for(int i = 0;i < parameters.length;++ i)
            matchedParams[i] = safeCast(parameterTypes[i], parameters[i]);
        return matchedParams;
    }
}