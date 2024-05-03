package aviel.crazy.parser;

import aviel.crazy.data.iter.Iter;
import aviel.crazy.data.pair.Pair;
import aviel.crazy.function.Func;
import aviel.crazy.function.Pred;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;

/**
 * indeterministic parser
 */
public record IndtParser<Elem, Val>(Func<List<Elem>, Iter<Pair<List<Elem>, Val>>> get) {
    public static <Elem, Val> IndtParser<Elem, Val> of(Func<List<Elem>, Iter<Pair<List<Elem>, Val>>> get) {
        return new IndtParser<>(get);
    }

    public static <Elem, Val extends SupVal, SupVal> IndtParser<Elem, SupVal> cast(IndtParser<Elem, Val> parser) {
        return parser.map(val -> val);
    }

    public Iter<Pair<List<Elem>, Val>> parse(List<Elem> input) {
        return get.apply(input);
    }

    public <MVal> IndtParser<Elem, MVal> map(Func<Val, MVal> mapper) {
        return of(get.map(Func.of(pairMaybe -> pairMaybe.map(pair -> pair.map(mapper)))));
    }

    public record Composer<Elem, Result, Creator>(Func<Creator, IndtParser<Elem, Result>> get) {
        public <Arg> Composer<Elem, Result, Func<Arg, Creator>> arg(IndtParser<Elem, Arg> arg) {
            return new Composer<>(newCreator -> arg.map(newCreator).bind(get));
        }

        public IndtParser<Elem, Result> apply(Creator creator) {
            return get.apply(creator);
        }
    }

    public static <Elem, Result> Composer<Elem, Result, Result> composer() {
        return new Composer<>(IndtParser::wrap);
    }

    public <MVal> IndtParser<Elem, MVal> bind(Func<Val, IndtParser<Elem, MVal>> action) {
        return IndtParser.of(in -> {
            Iter<Pair<List<Elem>, Val>> result = parse(in);
            return result.bind(pair -> action.apply(pair.right()).parse(pair.left()));
        });
    }

    public static <In, Val> IndtParser<In, Val> wrap(Val value) {
        return of(input -> Iter.wrap(Pair.of(input, value)));
    }

    public static <In, Val> IndtParser<In, Val> fail() {
        return of(Func.wrap(Iter.empty()));
    }

    public <OtherVal> IndtParser<Elem, OtherVal> ignoreTo(IndtParser<Elem, OtherVal> parser) {
        return this.bind(ignore -> parser);
    }

    public <OtherVal> IndtParser<Elem, OtherVal> ignoreTo(OtherVal parser) {
        return this.bind(ignore -> wrap(parser));
    }

    public <OtherVal> IndtParser<Elem, Val> ignoring(IndtParser<Elem, OtherVal> parser) {
        return this.bind(val -> parser.map(ignore -> val));
    }

    public IndtParser<Elem, Val> guard(Pred<Val> cond) {
        return of(elems -> parse(elems).filter(cond.comp(Pair::right)));
    }

    public IndtParser<Elem, Val> or(IndtParser<Elem, Val> other) {
        return of(Func.<List<Elem>, Iter<Pair<List<Elem>, Val>>>composer()
                      .arg(other.get)
                      .arg(this.get)
                      .apply(thing -> thing::or));
    }

    public static <Elem, Val, Vals> Collector<IndtParser<Elem, Val>, ?, IndtParser<Elem, Vals>>
    collect(Collector<Val, ?, Vals> collector) {
        return collect1(collector);
    }

    private static <Acc, Elem, Val, Vals> Collector<IndtParser<Elem, Val>, AtomicReference<IndtParser<Elem, Acc>>, IndtParser<Elem, Vals>>
    collect1(Collector<Val, Acc, Vals> collector) {
        return Collector.of(() -> new AtomicReference<>(IndtParser.wrap(collector.supplier().get())),
                            (ref, parser) -> ref.set(IndtParser.<Elem, Acc>composer()
                                                               .arg(parser)
                                                               .arg(ref.get())
                                                               .apply(acc -> val -> {
                                                                   collector.accumulator().accept(acc, val);
                                                                   return acc;
                                                               })),
                            (ref1, ref2) -> new AtomicReference<>(IndtParser.<Elem, Acc>composer()
                                                                            .arg(ref2.get())
                                                                            .arg(ref1.get())
                                                                            .apply(acc1 -> acc2 ->
                                                                                    collector.combiner().apply(acc1, acc2))),
                            ref -> ref.get().map(collector.finisher()::apply));
    }

    public <Vals> IndtParser<Elem, Vals> plus(Collector<Val, ?, Vals> collector) {
        return plus1(collector);
    }

    private <Acc, Vals> IndtParser<Elem, Vals> plus1(Collector<Val, Acc, Vals> collector) {
        return IndtParser.<Elem, Vals>composer()
                         .arg(IndtParser.of(input -> (
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

    public <Vals> IndtParser<Elem, Vals> star(Collector<Val, ?, Vals> collector) {
        return star1(collector);
    }

    private <Acc, Vals> IndtParser<Elem, Vals> star1(Collector<Val, Acc, Vals> collector) {
        return IndtParser.of((List<Elem> input) -> plus1(collector).parse(input))
                         .or(wrap(collector.finisher().apply(collector.supplier().get())));
    }

    public static <Elem> IndtParser<Elem, Elem> token() {
        return of(elems -> {
            if (elems.isEmpty()) {
                return Iter.empty();
            }
            return Iter.wrap(new Pair<>(elems.subList(1, elems.size()), elems.get(0)));
        });
    }
}
