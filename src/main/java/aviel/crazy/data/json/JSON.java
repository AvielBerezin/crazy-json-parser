package aviel.crazy.data.json;

public interface JSON {
    interface Dispatcher<Result> {
        Result with(JSONNull aNull);
        Result with(JSONBool bool);
        Result with(JSONNum num);
        Result with(JSONString string);
        Result with(JSONArray array);
        Result with(JSONObject object);
    }

    <Result> Result dispatch(Dispatcher<Result> dispatcher);
}
