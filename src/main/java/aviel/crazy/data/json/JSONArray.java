package aviel.crazy.data.json;

import java.util.List;

public record JSONArray(List<JSON> get) implements JSON {
    @Override
    public <Result> Result dispatch(Dispatcher<Result> dispatcher) {
        return dispatcher.with(this);
    }
}
