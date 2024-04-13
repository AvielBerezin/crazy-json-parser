package aviel.crazy.json_parser;

import aviel.crazy.data.json.JSONNull;
import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.pair.Pair;
import junit.framework.TestCase;

import java.util.List;

public class JsonParserTest extends TestCase {
    public void testNull() {
        assert JsonParser.jsonParser().parse(List.of('n', 'u', 'l', 'l'))
                         .equals(Maybe.wrap(Pair.of(List.of(), new JSONNull())));
    }
}