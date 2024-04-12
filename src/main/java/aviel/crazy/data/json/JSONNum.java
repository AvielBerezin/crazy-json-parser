package aviel.crazy.data.json;

public record JSONNum(Number get) implements JSON {
    @Override
    public <Result> Result dispatch(Dispatcher<Result> dispatcher) {
        return dispatcher.with(this);
    }
}
