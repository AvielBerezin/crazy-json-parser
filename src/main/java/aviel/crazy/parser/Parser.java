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

    public static <Elem, Val extends SupVal, SupVal> Parser<Elem, SupVal> cast(Parser<Elem, Val> parser) {
        return parser.map(val -> val);
    }

    public Maybe<Pair<List<Elem>, Val>> parse(List<Elem> input) {
        return get.apply(input);
    }

    public <MVal> Parser<Elem, MVal> map(Func<Val, MVal> mapper) {
        return of(get.map(Func.of(pairMaybe -> pairMaybe.map(pair -> pair.map(mapper)))));
    }

    public record Composer<Elem, Result, Creator>(Func<Creator, Parser<Elem, Result>> get) {
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
        return of(input -> Maybe.wrap(Pair.of(input, value)));
    }

    public static <In, Val> Parser<In, Val> fail() {
        return of(Func.wrap(new MaybeNone<>()));
    }

    public <OtherVal> Parser<Elem, OtherVal> ignoreTo(Parser<Elem, OtherVal> parser) {
        return this.bind(ignore -> parser);
    }

    public <OtherVal> Parser<Elem, OtherVal> ignoreTo(OtherVal parser) {
        return this.bind(ignore -> wrap(parser));
    }

    public <OtherVal> Parser<Elem, Val> ignoring(Parser<Elem, OtherVal> parser) {
        return this.bind(val -> parser.map(ignore -> val));
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
    collect(Collector<Val, ?, Vals> collector) {
        return collect1(collector);
    }

    private static <Acc, Elem, Val, Vals> Collector<Parser<Elem, Val>, AtomicReference<Parser<Elem, Acc>>, Parser<Elem, Vals>>
    collect1(Collector<Val, Acc, Vals> collector) {
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
                     .arg(Parser.of(input -> (
                                    star1(Collector.of(collector.supplier(),
                                          collector.accumulator(),
                                          collector.combiner()))
                                            .parse(input)
                     )))
                     .arg(this)
                     .apply(val -> acc2 -> {
                         Acc acc1 = collector.supplier().get();
                         collector.accumulator().accept(acc1, val);
                         return collector.finisher().apply(collector.combiner().apply(acc1, acc2));
                     });
    }

    public <Vals> Parser<Elem, Vals> star(Collector<Val, ?, Vals> collector) {
        return star1(collector);
    }

    private <Acc, Vals> Parser<Elem, Vals> star1(Collector<Val, Acc, Vals> collector) {
        return Parser.of((List<Elem> input) -> plus1(collector).parse(input))
                     .or(wrap(collector.finisher().apply(collector.supplier().get())));
    }

    public static <Elem> Parser<Elem, Elem> token() {
        return of(elems -> {
            if (elems.isEmpty()) {
                return new MaybeNone<>();
            }
            return new MaybeSome<>(new Pair<>(elems.subList(1, elems.size()), elems.get(0)));
        });
    }
}
