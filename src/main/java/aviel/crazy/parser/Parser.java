package aviel.crazy.parser;

import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.maybe.MaybeNone;
import aviel.crazy.data.maybe.MaybeSome;
import aviel.crazy.data.pair.Pair;
import aviel.crazy.function.Func;
import aviel.crazy.function.Pred;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

public record Parser<Elem, Val>(Func<List<Elem>, Maybe<Pair<List<Elem>, Val>>> get) {
    public static <Elem, Val> Parser<Elem, Val> of(Func<List<Elem>, Maybe<Pair<List<Elem>, Val>>> get) {
        return new Parser<>(get);
    }

    public Maybe<Pair<List<Elem>, Val>> parse(List<Elem> input) {
        return get.apply(input);
    }

    public <MVal> Parser<Elem, MVal> map(Func<Val, MVal> mapper) {
        return of(get.map(Func.of(pairMaybe -> pairMaybe.map(pair -> pair.map(mapper)))));
    }

    record Composer<Elem, Result, Creator>(Func<Creator, Parser<Elem, Result>> get) {
        public <Arg> Composer<Elem, Result, Func<Arg, Creator>> arg(Parser<Elem, Arg> arg) {
            return new Composer<>(newCreator -> arg.map(newCreator).bind(get));
        }

        public Parser<Elem, Result> apply(Creator creator) {
            return get.apply(creator);
        }
    }

    public static <Elem, Result> Composer<Elem, Result, Result> composer() {
        return new Composer<>(Parser::wrap);
    }

    public <MVal> Parser<Elem, MVal> bind(Func<Val, Parser<Elem, MVal>> action) {
        return Parser.of(in -> {
            Maybe<Pair<List<Elem>, Val>> result = parse(in);
            return result.bind(pair -> action.apply(pair.right()).parse(pair.left()));
        });
    }

    public static <In, Val> Parser<In, Val> wrap(Val value) {
        return of(Func.wrap(Maybe.wrap(Pair.of(Collections.emptyList(), value))));
    }

    public Parser<Elem, Val> guard(Pred<Val> cond) {
        return of(elems -> parse(elems).guard(cond.comp(Pair::right)));
    }

    public Parser<Elem, Val> or(Parser<Elem, Val> other) {
        return of(elems -> parse(elems).dispatch(new Maybe.Dispatcher<>() {
            @Override
            public Maybe<Pair<List<Elem>, Val>> with(MaybeNone<Pair<List<Elem>, Val>> none) {
                return other.parse(elems);
            }

            @Override
            public Maybe<Pair<List<Elem>, Val>> with(MaybeSome<Pair<List<Elem>, Val>> some) {
                return some;
            }
        }));
    }

    public static <Elem, Val, Vals> Collector<Parser<Elem, Val>, ?, Parser<Elem, Vals>>
    traverse(Collector<Val, ?, Vals> collector) {
        return traverse1(collector);
    }

    private static <Acc, Elem, Val, Vals> Collector<Parser<Elem, Val>, AtomicReference<Parser<Elem, Acc>>, Parser<Elem, Vals>>
    traverse1(Collector<Val, Acc, Vals> collector) {
        return Collector.of(() -> new AtomicReference<>(Parser.wrap(collector.supplier().get())),
                            (ref, parser) -> ref.set(Parser.<Elem, Acc>composer()
                                                           .arg(parser)
                                                           .arg(ref.get())
                                                           .apply(acc -> val -> {
                                                               collector.accumulator().accept(acc, val);
                                                               return acc;
                                                           })),
                            (ref1, ref2) -> new AtomicReference<>(Parser.<Elem, Acc>composer()
                                                                        .arg(ref2.get())
                                                                        .arg(ref1.get())
                                                                        .apply(acc1 -> acc2 ->
                                                                                collector.combiner().apply(acc1, acc2))),
                            ref -> ref.get().map(collector.finisher()::apply));
    }

    public <Vals> Parser<Elem, Vals> plus(Collector<Val, ?, Vals> collector) {
        return plus1(collector);
    }

    private <Acc, Vals> Parser<Elem, Vals> plus1(Collector<Val, Acc, Vals> collector) {
        return Parser.<Elem, Vals>composer()
                     .arg(star(Collector.of(collector.supplier(), collector.accumulator(), collector.combiner())))
                     .arg(this)
                     .apply(val -> acc -> {
                         collector.accumulator().accept(acc, val);
                         return collector.finisher().apply(acc);
                     });
    }

    public <Vals> Parser<Elem, Vals> star(Collector<Val, ?, Vals> collector) {
        return star1(collector);
    }

    private <Acc, Vals> Parser<Elem, Vals> star1(Collector<Val, Acc, Vals> collector) {
        return plus1(collector).or(wrap(collector.finisher().apply(collector.supplier().get())));
    }
}
