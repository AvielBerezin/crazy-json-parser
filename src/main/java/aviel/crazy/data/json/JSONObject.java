package aviel.crazy.data.json;

import java.util.Map;

public record JSONObject(Map<String, JSON> get) implements JSON {
    @Override
    public <Result> Result dispatch(Dispatcher<Result> dispatcher) {
        return dispatcher.with(this);
    }
}
