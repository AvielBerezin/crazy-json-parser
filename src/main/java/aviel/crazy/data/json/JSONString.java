package aviel.crazy.data.json;

public record JSONString(String get) implements JSON {
    @Override
    public <Result> Result dispatch(Dispatcher<Result> dispatcher) {
        return dispatcher.with(this);
    }
}
