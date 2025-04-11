package co.g3a.functionalrop.utils;

import java.util.function.Function;

/**
 * Representa una función que acepta un parámetro de entrada y retorna un resultado,
 * permitiendo el lanzamiento de excepciones verificadas.
 *
 * @param <In>  Tipo de entrada
 * @param <Out> Tipo de salida
 */
@FunctionalInterface
public interface DeadEndFunction<In, Out>  {

    /**
     * Aplica esta función al input dado.
     *
     * @param input valor de entrada
     * @return resultado de la función
     * @throws Exception si ocurre un error durante la ejecución
     */
   // @Override
    Out apply(In input) throws Exception;
}