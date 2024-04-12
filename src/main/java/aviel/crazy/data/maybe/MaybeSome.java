package aviel.crazy.data.maybe;

public record MaybeSome<Val>(Val get) implements Maybe<Val> {
    @Override
    public <Result> Result dispatch(Dispatcher<Val, Result> dispatcher) {
        return dispatcher.with(this);
    }
}
