package aviel.crazy;

import aviel.crazy.data.maybe.Maybe;

import java.util.List;

public class Scratch {
    record ParseResult<Elem, Val>(List<Elem> rest, Val value) {}

    @FunctionalInterface
    interface Parser<Elem, Val> {
        Maybe<ParseResult<Elem, Val>> parse(List<Elem> input);
    }
}
