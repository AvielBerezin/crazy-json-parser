package aviel.crazy.function;

@FunctionalInterface
public interface Func<In, Out> {
    Out apply(In in);

    default Supp<Out> partial(In in) {
        return () -> apply(in);
    }

    default <FromIn> Func<FromIn, Out> comp(Func<FromIn, In> other) {
        return fromIn -> apply(other.apply(fromIn));
    }

    default Supp<Out> comp(Supp<In> other) {
        return () -> apply(other.get());
    }

    default <MapOut> Func<In, MapOut> map(Func<Out, MapOut> mapper) {
        return mapper.comp(this);
    }

    default Cons<In> map(Cons<Out> mapper) {
        return mapper.comp(this);
    }

    default <MapOut> Func<In, MapOut> bind(Func<Out, Func<In, MapOut>> action) {
        return in -> action.apply(Func.this.apply(in))
                           .apply(in);
    }

    static <In, Val> Func<In, Val> wrap(Val value) {
        return in -> value;
    }
}
