package aviel.crazy.data.pair;

import aviel.crazy.function.Func;

import java.util.function.BiConsumer;
import java.util.stream.Collector;

public record Pair<L, R>(L left, R right) {
    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    public Pair<R, L> swap() {
        return Pair.of(right, left);
    }

    public <MapR> Pair<L, MapR> map(Func<R, MapR> mapper) {
        return new Pair<>(left, mapper.apply(right));
    }

    public record LeftCollected<Acc, L, R>(Collector<L, Acc, L> leftCollector,
                                           Pair<L, R> get) {
        public <MapR> LeftCollected<Acc, L, MapR> map(Func<R, MapR> mapper) {
            return new LeftCollected<>(leftCollector, get.map(mapper));
        }

        public record Composer<Acc, L, Result, Creator>(Collector<L, Acc, L> leftCollector,
                                                        Func<Creator, LeftCollected<Acc, L, Result>> get) {
            public <Arg> Composer<Acc, L, Result, Func<Arg, Creator>> arg(Pair<L, Arg> arg) {
                return new Composer<>(leftCollector,
                                      newCreator -> arg.<Creator>map(newCreator)
                                                       .leftCollected(leftCollector)
                                                       .bind(get.map(Func.of(LeftCollected::get))));
            }

            public LeftCollected<Acc, L, Result> apply(Creator creator) {
                return get.apply(creator);
            }
        }

        public <MapR> LeftCollected<Acc, L, MapR> bind(Func<R, Pair<L, MapR>> action) {
            BiConsumer<Acc, L> accumulator = leftCollector.accumulator();
            Acc acc = leftCollector.supplier().get();
            accumulator.accept(acc, get.left);
            Pair<L, MapR> afterAction = action.apply(get.right);
            accumulator.accept(acc, afterAction.left);
            Pair<L, MapR> resultPair = Pair.of(leftCollector.finisher().apply(acc),
                                               afterAction.right);
            return new LeftCollected<>(leftCollector, resultPair);
        }
    }

    public static <Acc, L, Result> LeftCollected.Composer<Acc, L, Result, Result>
    composer(Collector<L, Acc, L> leftCollector) {
        return new LeftCollected.Composer<>(leftCollector, result -> {
            L initial = leftCollector.finisher().apply(leftCollector.supplier().get());
            return Pair.of(initial, result).leftCollected(leftCollector);
        });
    }

    public <Acc> LeftCollected<Acc, L, R> leftCollected(Collector<L, Acc, L> leftCollector) {
        return new LeftCollected<>(leftCollector, this);
    }

    public static <L, ColL, R, ColR> Collector<Pair<L, R>, ?, Pair<ColL, ColR>> collect(Collector<L, ?, ColL> leftCollector,
                                                                                        Collector<R, ?, ColR> rightCollector) {
        return collect1(leftCollector, rightCollector);
    }

    private static <L, AccL, ColL, R, AccR, ColR> Collector<Pair<L, R>, Pair<AccL, AccR>, Pair<ColL, ColR>>
    collect1(Collector<L, AccL, ColL> leftCollector,
             Collector<R, AccR, ColR> rightCollector) {
        return Collector.of(() -> Pair.of(leftCollector.supplier().get(),
                                          rightCollector.supplier().get()),
                            (pairAcc, pair) -> {
                                leftCollector.accumulator().accept(pairAcc.left, pair.left);
                                rightCollector.accumulator().accept(pairAcc.right, pair.right);
                            },
                            (pairAcc1, pairAcc2) -> Pair.of(leftCollector.combiner().apply(pairAcc1.left, pairAcc2.left),
                                                            rightCollector.combiner().apply(pairAcc1.right, pairAcc2.right)),
                            pairAcc -> Pair.of(leftCollector.finisher().apply(pairAcc.left),
                                               rightCollector.finisher().apply(pairAcc.right)));
    }
}
