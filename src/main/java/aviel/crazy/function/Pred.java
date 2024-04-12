package aviel.crazy.function;

@FunctionalInterface
public interface Pred<In> extends Func<In, Boolean> {
    default Pred<In> and(Pred<In> other) {
        return in -> apply(in) && other.apply(in);
    }

    default Pred<In> or(Pred<In> other) {
        return in -> apply(in) || other.apply(in);
    }

    default Pred<In> not() {
        return in -> !apply(in);
    }

    default Pred<In> xor(Pred<In> other) {
        return in -> apply(in) ^ other.apply(in);
    }

    static <In> Pred<In> from(Func<In, Boolean> func) {
        return func::apply;
    }
}
