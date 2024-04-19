package aviel.crazy.json_parser;

import aviel.crazy.data.json.JSONBool;
import aviel.crazy.data.json.JSONNull;
import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.pair.Pair;
import aviel.crazy.function.Pred;
import junit.framework.TestCase;

import java.util.List;

import static aviel.crazy.utils.OtherUtils.listOfString;

public class JsonParserTest extends TestCase {
    public void testNull() {
        assertValue(JsonParser.jsonParser().parse(listOfString("null")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNull())));
    }

    public void testBool() {
        assertValue(JsonParser.jsonParser().parse(listOfString("true")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONBool(true))));
        assertValue(JsonParser.jsonParser().parse(listOfString("false")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONBool(false))));
    }

    record AssertedValue<Val>(Val value) {
        void satisfies(Pred<Val> condition) {
            if (!condition.apply(value)) {
                throw new AssertionError("value " + value + " did not satisfy condition");
            }
        }

        void isEqualTo(Val other) {
            if (!value.equals(other)) {
                throw new AssertionError("value " + value + " did not equate " + other);
            }
        }
    }

    static <Val> AssertedValue<Val> assertValue(Val value) {
        return new AssertedValue<>(value);
    }
}