package aviel.crazy.function;

@FunctionalInterface
public interface Pred<In> extends Func<In, Boolean> {
    static <In> Pred<In> of(Pred<In> pred) {
        return pred;
    }

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

    @Override
    default <FromIn> Pred<FromIn> comp(Func<FromIn, In> other) {
        return from(Func.super.comp(other));
    }
}
