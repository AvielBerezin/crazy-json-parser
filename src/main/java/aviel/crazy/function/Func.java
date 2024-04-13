package aviel.crazy.function;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

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

    static <In, Val, Vals> Collector<Func<In, Val>, ?, Func<In, Vals>> collect(Collector<Val, ?, Vals> collector) {
        return collect1(collector);
    }

    private static <Acc, In, Val, Vals> Collector<Func<In, Val>, AtomicReference<Func<In, Acc>>, Func<In, Vals>>
    collect1(Collector<Val, Acc, Vals> collector) {
        return Collector.of(() -> new AtomicReference<>(Func.wrap(collector.supplier().get())),
                            (ref, parser) -> ref.set(Func.<In, Acc>composer()
                                                           .arg(parser)
                                                           .arg(ref.get())
                                                           .apply(acc -> val -> {
                                                               collector.accumulator().accept(acc, val);
                                                               return acc;
                                                           })),
                            (ref1, ref2) -> new AtomicReference<>(Func.<In, Acc>composer()
                                                                        .arg(ref2.get())
                                                                        .arg(ref1.get())
                                                                        .apply(acc1 -> acc2 ->
                                                                                collector.combiner().apply(acc1, acc2))),
                            ref -> ref.get().map(collector.finisher()::apply));
    }
}
