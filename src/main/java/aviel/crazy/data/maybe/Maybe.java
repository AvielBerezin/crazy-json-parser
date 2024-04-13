package aviel.crazy.data.maybe;

import aviel.crazy.function.Cons;
import aviel.crazy.function.Func;
import aviel.crazy.function.Pred;
import aviel.crazy.function.Runner;

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
            return get.apply(creator);
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

    static <Val, ColVal> Collector<Maybe<Val>, ?, Maybe<ColVal>> collect(Collector<Val, ?, ColVal> collector) {
        return collect1(collector);
    }

    private static <Val, Acc, ColVal> Collector<Maybe<Val>, Maybe<Acc>, Maybe<ColVal>> collect1(Collector<Val, Acc, ColVal> collector) {
        return Collector.of(() -> wrap(collector.supplier().get()),
                            (mayAcc, mayVal) -> Maybe.<Runner>composer()
                                                     .arg(mayVal)
                                                     .arg(mayAcc)
                                                     .apply(acc -> val -> () -> collector.accumulator().accept(acc, val))
                                                     .ifSome(Runner::run),
                            (may1, may2) -> Maybe.<Acc>composer()
                                                 .arg(may2)
                                                 .arg(may1)
                                                 .apply(acc1 -> acc2 ->
                                                         collector.combiner().apply(acc1, acc2)),
                            ref -> ref.map(collector.finisher()::apply));
    }

    default void ifSome(Cons<Val> doIfSome) {
        dispatch(new Dispatcher<Val, Runner>() {
            @Override
            public Runner with(MaybeNone<Val> none) {
                return () -> {};
            }

            @Override
            public Runner with(MaybeSome<Val> some) {
                return doIfSome.partial(some.get());
            }
        }).run();
    }
}
