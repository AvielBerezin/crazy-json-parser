package aviel.crazy.function;

@FunctionalInterface
public interface Cons<In> {
    void accept(In in);

    static <In> Cons<In> of(Cons<In> cons) {
        return cons;
    }

    default Runner partial(In in) {
        return () -> accept(in);
    }

    default <FromIn> Cons<FromIn> comp(Func<FromIn, In> other) {
        return fromIn -> accept(other.apply(fromIn));
    }

    default Runner comp(Supp<In> other) {
        return () -> accept(other.get());
    }

    default <Out> Func<In, Out> map(Supp<Out> mapper) {
        return mapper.comp(this);
    }

    default Cons<In> map(Runner mapper) {
        return mapper.comp(this);
    }
}
