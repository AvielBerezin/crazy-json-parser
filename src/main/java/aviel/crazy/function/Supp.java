package aviel.crazy.function;

@FunctionalInterface
public interface Supp<Out> {
    Out get();

    default <In> Func<In, Out> comp(Cons<In> other) {
        return in -> {
            other.accept(in);
            return get();
        };
    }

    default Supp<Out> comp(Runner runner) {
        return () -> {
            runner.run();
            return get();
        };
    }

    default <MapOut> Supp<MapOut> map(Func<Out, MapOut> mapper) {
        return mapper.comp(this);
    }

    default Runner map(Cons<Out> mapper) {
        return mapper.comp(this);
    }
}
