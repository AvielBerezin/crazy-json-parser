package aviel.crazy;

import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.pair.Pair;
import aviel.crazy.function.Func;

import java.util.List;

public class Scratch {
    record ParseResult<Elem, Val>(List<Elem> rest, Val value) {}

    @FunctionalInterface
    interface Parser<Elem, Val> {
        Maybe<ParseResult<Elem, Val>> parse(List<Elem> input);
    }

    record ParserR<Elem, Val>(Func<List<Elem>, Maybe<Pair<List<Elem>, Val>>> get) {
        public Maybe<Pair<List<Elem>, Val>> parse(List<Elem> input) {
            return get.apply(input);
        }


    }
}
