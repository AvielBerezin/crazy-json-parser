package aviel.crazy.function;

@FunctionalInterface
public interface Runner {
    void run();

    default <In> Cons<In> comp(Cons<In> other) {
        return in -> {
            other.accept(in);
            run();
        };
    }

    default Runner comp(Runner other) {
        return () -> {
            other.run();
            run();
        };
    }

    default <Out> Supp<Out> map(Supp<Out> mapper) {
        return mapper.comp(this);
    }

    default Runner map(Runner mapper) {
        return mapper.comp(this);
    }
}
