package co.g3a.functionalrop;

@FunctionalInterface
public interface DeadEndFunction<In, Out> {
    Out apply(In input) throws Exception;
}