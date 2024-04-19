package aviel.crazy.json_parser;

import aviel.crazy.data.json.*;
import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.maybe.MaybeNone;
import aviel.crazy.data.maybe.MaybeSome;
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

    public void testNum() {
        assertValue(JsonParser.jsonParser().parse(listOfString("123")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(123))));
        assertValue(JsonParser.jsonParser().parse(listOfString("13.")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(13d))));
        assertValue(JsonParser.jsonParser().parse(listOfString("-13.")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(-13d))));
        assertValue(JsonParser.jsonParser().parse(listOfString("-4")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(-4))));
        assertValue(JsonParser.jsonParser().parse(listOfString("-1e2")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(-100d))));
        assertValue(JsonParser.jsonParser().parse(listOfString("-1E2")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(-100d))));
        assertValue(JsonParser.jsonParser().parse(listOfString("+1e-2")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONNum(0.01d))));
        assertValue(JsonParser.jsonParser().parse(listOfString("-3.14e-1")))
                .satisfies(x -> x.dispatch(new Maybe.Dispatcher<>() {
                    @Override
                    public Boolean with(MaybeNone<Pair<List<Character>, JSON>> none) {return false;}

                    @Override
                    public Boolean with(MaybeSome<Pair<List<Character>, JSON>> some) {
                        return some.get().left().isEmpty() &&
                               some.get().right().dispatch(new JSON.Dispatcher<Boolean>() {
                                   @Override
                                   public Boolean with(JSONNull aNull) {return false;}

                                   @Override
                                   public Boolean with(JSONBool bool) {return false;}

                                   @Override
                                   public Boolean with(JSONNum num) {
                                       return num.get() instanceof Double d &&
                                              Math.abs(d - (-0.314)) < 1e-8;
                                   }

                                   @Override
                                   public Boolean with(JSONString string) {return false;}

                                   @Override
                                   public Boolean with(JSONArray array) {return false;}

                                   @Override
                                   public Boolean with(JSONObject object) {return false;}
                               });
                    }
                }));
    }

    public void testString() {
        assertValue(JsonParser.jsonParser().parse(listOfString("\"\"")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONString(""))));
        assertValue(JsonParser.jsonParser().parse(listOfString("\"abcd 1234\"")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONString("abcd 1234"))));
        assertValue(JsonParser.jsonParser().parse(listOfString("\"\\n\\t\\\"\\\\\"")))
                .isEqualTo(Maybe.wrap(Pair.of(List.of(), new JSONString("\n\t\"\\"))));

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