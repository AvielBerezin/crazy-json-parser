package aviel.crazy.json_parser;


import aviel.crazy.data.MutableBox;
import aviel.crazy.data.iter.Iter;
import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.pair.Pair;
import aviel.crazy.function.Supp;
import aviel.crazy.parser.IndtParser;
import aviel.crazy.parser.Parser;
import aviel.crazy.utils.OtherUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegexParser {
    private static Parser<Character, Character> parseA(char token) {
        return Parser.<Character>token().guard(c -> c == token);
    }

    private static Parser<Character, IndtParser<Character, String>> character() {
        return Parser.<Character>token()
                     .guard(c -> c != '\\')
                     .guard(c -> c != '|')
                     .guard(c -> c != '*')
                     .guard(c -> c != '(')
                     .guard(c -> c != ')')
                     .or(parseA('\\').ignoreTo(Stream.of('\\', '|', '*', '(', ')')
                                                     .map(RegexParser::parseA)
                                                     .reduce(Parser::or)
                                                     .orElse(Parser.fail())))
                     .map(c -> IndtParser.<Character>token()
                                         .guard(rc -> rc == c)
                                         .map(Objects::toString));
    }

    private static Parser<Character, IndtParser<Character, String>> or() {
        return Parser.<Character, IndtParser<Character, String>>composer()
                     .arg(Parser.of(elems -> regex().parse(elems)))
                     .ignore(parseA('|'))
                     .arg(Parser.of(elems -> regex().parse(elems)))
                     .apply(thing -> thing::or);
    }

    private static Parser<Character, IndtParser<Character, String>> cat() {
        return Parser.<Character, IndtParser<Character, String>>composer()
                     .arg(Parser.of(elems -> regex().parse(elems)))
                     .arg(Parser.of(elems -> regex().parse(elems)))
                     .apply(re1 -> re2 ->
                             IndtParser.<Character, String>composer()
                                       .arg(re2)
                                       .arg(re1)
                                       .apply(s1 -> s2 -> s1 + s2));
    }

    private static Parser<Character, IndtParser<Character, String>> star() {
        return Parser.<Character, IndtParser<Character, String>>composer()
                     .arg(Parser.of(elems -> regex().parse(elems)))
                     .ignore(parseA('*'))
                     .apply(re -> re.star(Collectors.mapping(x -> x, Collectors.joining())));
    }

    private static Parser<Character, IndtParser<Character, String>> regex() {
        return character().or(or()).or(cat()).or(star());
    }

    public static <Val> List<Val> listOfIter(Iter<Val> iter) {
        ListOfIterState<Val> state = new ListOfIterStateIterList<>(iter, new LinkedList<>());
        MutableBox<ListOfIterStateDone<Val>> nullableDone = new MutableBox<>(null);
        do {
            state = state.next();
            state.asDone().ifSome(nullableDone::set);
        }
        while (nullableDone.get() == null);
        return nullableDone.get().result();
    }

    private interface ListOfIterState<Val> {
        default Maybe<ListOfIterStateDone<Val>> asDone() {
            return Maybe.none();
        }

        ListOfIterState<Val> next();
    }

    private record ListOfIterStateIterList<Val>(Iter<Val> iter, List<Val> result) implements ListOfIterState<Val> {
        @Override
        public ListOfIterState<Val> next() {
            iter.next().ifSome(result::add);
            return this;
        }
    }


    private record ListOfIterStateDone<Val>(List<Val> result) implements ListOfIterState<Val> {
        @Override
        public Maybe<ListOfIterStateDone<Val>> asDone() {
            return Maybe.some(this);
        }

        @Override
        public ListOfIterStateDone<Val> next() {
            return this;
        }
    }

    public static void main(String[] args) {
        System.out.println(listOfIter(regex().parse(OtherUtils.listOfString("abcd"))
                                             .map(Supp::wrap)
                                             .orElse(() -> {throw new RuntimeException("not a regex");})
                                             .get()
                                             .right()
                                             .parse(OtherUtils.listOfString("abcd"))
                                             .map(Pair::right)));
    }
}
