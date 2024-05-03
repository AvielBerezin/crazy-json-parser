package aviel.crazy.function;

@FunctionalInterface
public interface Supp<Out> {
    Out get();

    static <Out> Supp<Out> of(Supp<Out> supp) {
        return supp;
    }

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

    default <MapOut> Supp<MapOut> bind(Func<Out, Supp<MapOut>> action) {
        return () -> action.apply(Supp.this.get()).get();
    }

    static <Val> Supp<Val> wrap(Val value) {
        return () -> value;
    }

    record Composer<Result, Creator>(Func<Creator, Supp<Result>> get) {
        public <Arg> Composer<Result, Func<Arg, Creator>> arg(Supp<Arg> arg) {
            return new Composer<>(newCreator -> arg.map(newCreator).bind(get));
        }

        public Supp<Result> apply(Creator creator) {
            return get.apply(creator);
        }
    }

    static <Result> Composer<Result, Result> composer() {
        return new Composer<>(Supp::wrap);
    }
}
