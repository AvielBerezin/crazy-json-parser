package aviel.crazy.data.maybe;

import aviel.crazy.function.Func;
import aviel.crazy.function.Pred;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

public interface Maybe<Val> {
    interface Dispatcher<Val, Result> {
        Result with(MaybeNone<Val> none);
        Result with(MaybeSome<Val> some);
    }

    <Result> Result dispatch(Dispatcher<Val, Result> dispatcher);

    default <MapVal> Maybe<MapVal> map(Func<Val, MapVal> mapper) {
        return dispatch(new Dispatcher<>() {
            @Override
            public Maybe<MapVal> with(MaybeNone<Val> none) {
                return new MaybeNone<>();
            }

            @Override
            public Maybe<MapVal> with(MaybeSome<Val> some) {
                return new MaybeSome<>(mapper.apply(some.get()));
            }
        });
    }

    record Composer<Result, Creator>(Func<Creator, Maybe<Result>> get) {
        public <Val> Composer<Result, Func<Val, Creator>> arg(Maybe<Val> val) {
            return new Composer<>(newCreator -> val.map(newCreator).bind(get));
        }

        public Maybe<Result> apply(Creator creator) {
            return get().apply(creator);
        }

    }

    static <Result> Composer<Result, Result> composer() {
        return new Composer<>(Maybe::wrap);
    }

    default <MapVal> Maybe<MapVal> bind(Func<Val, Maybe<MapVal>> action) {
        return dispatch(new Dispatcher<>() {
            @Override
            public Maybe<MapVal> with(MaybeNone<Val> none) {
                return new MaybeNone<>();
            }

            @Override
            public Maybe<MapVal> with(MaybeSome<Val> some) {
                return action.apply(some.get());
            }
        });
    }

    static <Val> MaybeSome<Val> wrap(Val value) {
        return new MaybeSome<>(value);
    }

    default Maybe<Val> guard(Pred<Val> cond) {
        return dispatch(new Dispatcher<>() {
            @Override
            public Maybe<Val> with(MaybeNone<Val> none) {
                return new MaybeNone<>();
            }

            @Override
            public Maybe<Val> with(MaybeSome<Val> some) {
                if (cond.apply(some.get())) {
                    return some;
                }
                return new MaybeNone<>();
            }
        });
    }

    static <Val, ColVal> Collector<Maybe<Val>, ?, Maybe<ColVal>> traverse(Collector<Val, ?, ColVal> collector) {
        return traverse1(collector);
    }

    private static <Val, Acc, ColVal> Collector<Maybe<Val>, AtomicReference<Maybe<Acc>>, Maybe<ColVal>> traverse1(Collector<Val, Acc, ColVal> collector) {
        return Collector.of(AtomicReference::new,
                            (ref, may) -> ref.set(Maybe.<Acc>composer()
                                                       .arg(may)
                                                       .arg(ref.get())
                                                       .apply(acc -> val -> {
                                                           collector.accumulator().accept(acc, val);
                                                           return acc;
                                                       })),
                            (ref1, ref2) -> new AtomicReference<>(Maybe.<Acc>composer()
                                                                       .arg(ref2.get())
                                                                       .arg(ref1.get())
                                                                       .apply(acc1 -> acc2 ->
                                                                               collector.combiner().apply(acc1, acc2))),
                            ref -> ref.get().map(collector.finisher()::apply));
    }
}
