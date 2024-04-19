package aviel.crazy.json_parser;

import aviel.crazy.data.json.*;
import aviel.crazy.function.Func;
import aviel.crazy.parser.Parser;
import aviel.crazy.utils.MoreCollectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static aviel.crazy.parser.Parser.cast;
import static aviel.crazy.parser.Parser.wrap;
import static aviel.crazy.utils.OtherUtils.listOfString;

public class JsonParser {
    private static Parser<Character, Character> whitespace() {
        return Parser.<Character>token().guard(c -> c <= 32);
    }

    private static Parser<Character, Object> padding() {
        return whitespace().star(Collector.of(() -> null, (a, b) -> {}, (a, b) -> null));
    }

    public static Parser<Character, JSONNull> jsonNullParser() {
        return padding().ignoreTo(listOfString("null")
                                          .stream()
                                          .map(c -> Parser.<Character>token().guard(c::equals))
                                          .collect(Parser.collect(MoreCollectors.ignoring()))
                                          .ignoreTo(new JSONNull()))
                        .ignoring(padding());
    }

    public static Parser<Character, JSONBool> jsonBoolParser() {
        return padding().ignoreTo(listOfString("true")
                                          .stream()
                                          .map(c -> Parser.<Character>token().guard(c::equals))
                                          .collect(Parser.collect(MoreCollectors.ignoring()))
                                          .ignoreTo(true)
                                          .or(listOfString("false")
                                                      .stream()
                                                      .map(c -> Parser.<Character>token().guard(c::equals))
                                                      .collect(Parser.collect(MoreCollectors.ignoring()))
                                                      .ignoreTo(false))
                                          .map(JSONBool::new))
                        .ignoring(padding());
    }

    private static <Value> Parser<Character, Func<Value, Value>> sign(Func<Value, Value> negation) {
        return Parser.<Character>token()
                     .guard(c -> c == '+')
                     .ignoreTo(Func.<Value, Value>of(x -> x))
                     .or(Parser.<Character>token()
                               .guard(c -> c == '-')
                               .ignoreTo(negation))
                     .or(wrap(x -> x));
    }

    public static Parser<Character, JSONNum> jsonNumParser() {
        Parser<Character, Integer> nonZeroDigit =
                Parser.<Character>token()
                      .guard(c -> '1' <= c && c <= '9')
                      .map(c -> c - '0');
        Parser<Character, Integer> digit =
                Parser.<Character>token()
                      .guard(c -> '0' <= c && c <= '9')
                      .map(c -> c - '0');
        Parser<Character, Integer> natural =
                nonZeroDigit.bind(lead -> digit.star(Collectors.toList())
                                               .map(digs -> digs.stream()
                                                                .reduce(lead, (res, d) -> res * 10 + d)));
        Parser<Character, Integer> whole =
                JsonParser.<Integer>sign(x1 -> -x1)
                          .bind(natural::map);
        Parser<Character, Double> cleanFloating =
                natural.bind(wholePart -> (
                        Parser.<Character>token()
                              .guard(c -> c == '.')
                              .ignoreTo(digit.star(Collectors.toCollection(ArrayList::new))
                                             .map(digs -> {
                                                 Collections.reverse(digs);
                                                 return digs;
                                             })
                                             .map(digs -> digs.stream()
                                                              .map(x -> (double) x)
                                                              .reduce(0d, (res, d) -> (res + d) / 10d))))
                        .map(fractPart -> wholePart + fractPart));
        Parser<Character, Double> floating =
                JsonParser.<Double>sign(x -> -x)
                          .bind(cleanFloating::map);
        Parser<Character, Double> scientific =
                JsonParser.<Double>sign(x -> -x)
                          .bind(cleanFloating.or(natural.map(x -> (double) x))
                                             .bind(precision -> Parser.<Character>token()
                                                                      .guard(c -> c == 'e' || c == 'E')
                                                                      .ignoreTo(whole)
                                                                      .map(exp -> precision * Math.pow(10, exp)))
                                        ::map);
        return padding().ignoreTo(Parser.<Character, Double, Number>cast(scientific)
                                        .or(cast(floating))
                                        .or(cast(whole)))
                        .map(JSONNum::new)
                        .ignoring(padding());
    }

    public static Parser<Character, JSONString> jsonStringParser() {
        Parser<Character, Character> doubleQuote =
                Parser.<Character>token()
                      .guard(c -> c == '\"');
        Parser<Character, Character> nonEscape =
                Parser.<Character>token()
                      .guard(c -> c != '\\' &&
                                  c != '\"');
        Parser<Character, Character> escaped =
                Parser.<Character>token()
                      .guard(c -> c == '\\')
                      .ignoreTo(Parser.<Character>token()
                                      .guard(c -> c == 'n')
                                      .ignoreTo('\n')
                                      .or(Parser.<Character>token()
                                                .guard(c -> c == 't')
                                                .ignoreTo('\t'))
                                      .or(Parser.<Character>token()
                                                .guard(c -> c == '\\'))
                                      .or(Parser.<Character>token()
                                                .guard(c -> c == '\"')));
        return padding().ignoreTo(doubleQuote)
                        .ignoreTo(nonEscape.or(escaped)
                                           .map(x -> (CharSequence) x.toString())
                                           .star(Collectors.joining())
                                           .map(JSONString::new))
                        .ignoring(doubleQuote)
                        .ignoring(padding());
    }

    public static Parser<Character, JSONArray> jsonArrayParser() {
        return Parser.fail();
    }

    public static Parser<Character, JSONObject> jsonObjectParser() {
        return Parser.fail();
    }

    public static Parser<Character, JSON> jsonParser() {
        return padding().ignoreTo(Parser.<Character, JSONNull, JSON>cast(jsonNullParser())
                                        .or(cast(jsonBoolParser()))
                                        .or(cast(jsonNumParser()))
                                        .or(cast(jsonStringParser()))
                                        .or(cast(jsonArrayParser()))
                                        .or(cast(jsonObjectParser())))
                        .ignoring(padding());
    }
}
