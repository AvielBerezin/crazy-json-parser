package aviel.crazy.function;

@FunctionalInterface
public interface Func<In, Out> {
    Out apply(In in);

    static <In, Out> Func<In, Out> of(Func<In, Out> func) {
        return func;
    }

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

    record Composer<In, Result, Creator>(Func<Creator, Func<In, Result>> get) {
        public <Arg> Composer<In, Result, Func<Arg, Creator>> arg(Func<In, Arg> arg) {
            return new Composer<>(newCreator -> arg.map(newCreator).bind(get));
        }

        public Func<In, Result> apply(Creator creator) {
            return get.apply(creator);
        }
    }

    static <In, Result> Composer<In, Result, Result> composer() {
        return new Composer<>(Func::wrap);
    }
}
