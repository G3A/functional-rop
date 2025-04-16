package co.g3a.functionalrop.utils;

@FunctionalInterface
public interface BiFunc<T, U, R> {
    R apply(T t, U u);
}