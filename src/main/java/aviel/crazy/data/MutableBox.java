package aviel.crazy.data;

import aviel.crazy.function.Func;

public class MutableBox<Val> {
    private Val val;

    public MutableBox(Val value) {
        val = value;
    }

    public Val get() {
        return val;
    }

    public void set(Val newValue) {
        this.val = newValue;
    }

    public void modify(Func<Val, Val> mapper) {
        this.val = mapper.apply(val);
    }
}
