package aviel.crazy.data.maybe;

public record MaybeNone<Val>() implements Maybe<Val> {
    @Override
    public <Result> Result dispatch(Dispatcher<Val, Result> dispatcher) {
        return dispatcher.with(this);
    }
}
