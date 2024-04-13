package aviel.crazy.json_parser;

import aviel.crazy.data.json.*;
import aviel.crazy.parser.Parser;

import java.util.stream.Collector;

import static aviel.crazy.parser.Parser.cast;

public class JsonParser {
    private static Parser<Character, Character> whitespace() {
        return Parser.<Character>token().guard(c -> c <= 32);
    }

    private static Parser<Character, Object> padding() {
        return whitespace().star(Collector.of(() -> null, (a, b) -> {}, (a, b) -> null));
    }

    public static Parser<Character, JSONNull> jsonNullParser() {
        return Parser.fail();
    }

    public static Parser<Character, JSONBool> jsonBoolParser() {
        return Parser.fail();
    }

    public static Parser<Character, JSONNum> jsonNumParser() {
        return Parser.fail();
    }

    public static Parser<Character, JSONString> jsonStringParser() {
        return Parser.fail();
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
