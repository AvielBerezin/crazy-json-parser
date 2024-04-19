package aviel.crazy;

import aviel.crazy.data.json.*;
import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.data.pair.Pair;
import aviel.crazy.json_parser.JsonParser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aviel.crazy.utils.OtherUtils.listOfString;

public class Scratch {

    public static final String stringLengthThreshold = "-----------------------------------------------";

    public static String jsonFormatter(JSON json) {
        return json.dispatch(new JSON.Dispatcher<String>() {
            @Override
            public String with(JSONNull aNull) {
                return "null";
            }

            @Override
            public String with(JSONBool bool) {
                return Boolean.toString(bool.get());
            }

            @Override
            public String with(JSONNum num) {
                return num.get().toString();
            }

            @Override
            public String with(JSONString string) {
                return "\"" +
                       string.get()
                             .replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\"))
                             .replaceAll(Pattern.quote("\n"), Matcher.quoteReplacement("\\n"))
                             .replaceAll(Pattern.quote("\t"), Matcher.quoteReplacement("\\t"))
                             .replaceAll(Pattern.quote("\""), Matcher.quoteReplacement("\\\"")) +
                       "\"";

            }

            @Override
            public String with(JSONArray array) {
                List<String> formatted = array.get()
                                              .stream()
                                              .map(Scratch::jsonFormatter)
                                              .toList();
                if (formatted.stream().noneMatch(x -> x.contains("\n"))) {
                    String shortForm = String.join(", ", formatted);
                    if (shortForm.length() < stringLengthThreshold.length()) {
                        return "[" + shortForm + "]";
                    }
                }
                return "[ " +
                       String.join("\n, ", formatted.stream()
                                                    .map(str -> str.replaceAll(Pattern.quote("\n"),
                                                                               Matcher.quoteReplacement("\n  ")))
                                                    .toList()) +
                       "\n]";
            }

            @Override
            public String with(JSONObject object) {
                List<Pair<String, String>> formatted = object.get().entrySet()
                                                             .stream()
                                                             .map(stringJSONEntry -> Pair.of(jsonFormatter(new JSONString(stringJSONEntry.getKey())),
                                                                                             jsonFormatter(stringJSONEntry.getValue())))
                                                             .toList();
                if (formatted.stream().map(Pair::right).noneMatch(str -> str.contains("\n"))) {
                    String shortForm = String.join(", ", formatted.stream().map(pair -> pair.left() + ": " + pair.right()).toList());
                    if (shortForm.length() < stringLengthThreshold.length()) {
                        return "{" + shortForm + "}";
                    }
                }
                return "{ " +
                       String.join("\n, ",
                                   formatted.stream()
                                            .map(pair -> pair.left() +
                                                         ":\n    " +
                                                         pair.right()
                                                             .replaceAll(Pattern.quote("\n"),
                                                                         Matcher.quoteReplacement("\n    ")))
                                            .toList()) +
                       "\n}";
            }
        });
    }

    public static void main(String[] args) {
        String jsonAsString = "{\n" +
                              "    \"glossary\": {\n" +
                              "        \"title\": \"example glossary\",\n" +
                              "\t\t\"GlossDiv\": {\n" +
                              "            \"title\": \"S\",\n" +
                              "\t\t\t\"GlossList\": {\n" +
                              "                \"GlossEntry\": {\n" +
                              "                    \"ID\": \"SGML\",\n" +
                              "\t\t\t\t\t\"SortAs\": \"SGML\",\n" +
                              "\t\t\t\t\t\"GlossTerm\": \"Standard Generalized Markup Language\",\n" +
                              "\t\t\t\t\t\"Acronym\": \"SGML\",\n" +
                              "\t\t\t\t\t\"Abbrev\": \"ISO 8879:1986\",\n" +
                              "\t\t\t\t\t\"GlossDef\": {\n" +
                              "                        \"para\": \"A meta-markup language, used to create markup languages such as DocBook.\",\n" +
                              "\t\t\t\t\t\t\"GlossSeeAlso\": [\"GML\", \"XML\"]\n" +
                              "                    },\n" +
                              "\t\t\t\t\t\"GlossSee\": \"markup\"\n" +
                              "                }\n" +
                              "            }\n" +
                              "        }\n" +
                              "    }\n" +
                              "}";
        Maybe<Pair<List<Character>, JSON>> json = JsonParser.jsonParser().parse(listOfString(jsonAsString));
        System.out.println(json);
        json.map(Pair::right)
            .map(Scratch::jsonFormatter)
            .ifSome(System.out::println);
    }
}
