package aviel.crazy.data.iter;

import aviel.crazy.data.MutableBox;
import aviel.crazy.data.maybe.Maybe;
import aviel.crazy.function.Func;
import aviel.crazy.function.Pred;
import aviel.crazy.function.Runner;
import aviel.crazy.function.Supp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Iterator of all elements that are some-s of the underlying supplier.
 * This iterator is based on side effects for the purpose of efficiency.
 */
public record Iter<Val>(Supp<Maybe<Val>> get) {
    public Maybe<Val> next() {
        return get.get();
    }

    public static <Val> Iter<Val> of(Supp<Maybe<Val>> supp) {
        return new Iter<>(supp);
    }

    public <MVal> Iter<MVal> map(Func<Val, MVal> mapper) {
        return Iter.of(get.map((Maybe<Val> valMaybe) -> valMaybe.map(mapper)));
    }

    public static <Val> Iter<Val> join(Iter<Iter<Val>> nested) {
        AtomicReference<JoinState<Val>> state = new AtomicReference<>(new JoinState_IterIter<>(nested));
        return of(() -> {
            state.set(state.get().next());
            AtomicReference<Maybe<Val>> nullableNext = new AtomicReference<>();
            do state.get().dispatch(new JoinState.Dispatcher<Val, Runner>() {
                @Override
                public Runner with(JoinState_Done<Val> done) {
                    return () -> nullableNext.set(Maybe.none());
                }

                @Override
                public Runner with(JoinState_InProgress<Val> inProgress) {
                    return () -> inProgress.takeVal().map(Maybe::some).ifSome(nullableNext::set);
                }
            }).run();
            while (nullableNext.get() == null);
            return nullableNext.get();
        });
    }

    private interface JoinState<Val> {
        interface Dispatcher<Val, Result> {
            Result with(JoinState_Done<Val> done);
            Result with(JoinState_InProgress<Val> inProgress);
        }

        <Result> Result dispatch(Dispatcher<Val, Result> dispatcher);

        default Maybe<Val> takeVal() {
            return Maybe.none();
        }

        JoinState<Val> next();
    }

    private interface JoinState_InProgress<Val> extends JoinState<Val> {
        @Override
        default <Result> Result dispatch(JoinState.Dispatcher<Val, Result> dispatcher) {
            return dispatcher.with(this);
        }
    }

    private record JoinState_Done<Val>() implements JoinState<Val> {
        @Override
        public <Result> Result dispatch(Dispatcher<Val, Result> dispatcher) {
            return dispatcher.with(this);
        }

        @Override
        public JoinState_Done<Val> next() {
            return this;
        }
    }

    private record JoinState_IterIter<Val>(Iter<Iter<Val>> iterIter) implements JoinState_InProgress<Val> {
        @Override
        public JoinState<Val> next() {
            return iterIter.next()
                           .<JoinState<Val>>map(iter -> new JoinState_IterIter_Iter<>(iterIter, iter))
                           .orElse(new JoinState_Done<>());
        }
    }

    private record JoinState_IterIter_Iter<Val>(Iter<Iter<Val>> iterIter,
                                                Iter<Val> iter)
            implements JoinState_InProgress<Val> {
        @Override
        public JoinState<Val> next() {
            return iter.next()
                       .<JoinState<Val>>map(val -> new JoinState_IterIter_Iter_Val<>(iterIter, iter, val))
                       .orElse(new JoinState_IterIter<>(iterIter));
        }
    }

    private record JoinState_IterIter_Iter_Val<Val>(Iter<Iter<Val>> iterIter,
                                                    Iter<Val> iter,
                                                    Val val)
            implements JoinState_InProgress<Val> {
        @Override
        public JoinState<Val> next() {
            return new JoinState_IterIter_Iter<>(iterIter, iter);
        }

        @Override
        public Maybe<Val> takeVal() {
            return Maybe.some(val);
        }
    }

    public static <Val> Iter<Val> wrap(Val value) {
        AtomicBoolean first = new AtomicBoolean(true);
        return Iter.of(() -> first.compareAndSet(true, false) ? Maybe.some(value) : Maybe.none());
    }

    public static <Val> Iter<Val> empty() {
        return Iter.of(Maybe::none);
    }

    public Iter<Val> filter(Pred<Val> condition) {
        FilterState<Val> start = new FilterState_Iter<>(condition, this);
        MutableBox<FilterStateResulted<Val>> state = new MutableBox<>(null);
        AtomicBoolean first = new AtomicBoolean(true);
        return of(() -> {
            if (first.compareAndSet(true, false)) {
                state.set(start.nextResulted());
            } else {
                state.modify(FilterState::nextResulted);
            }
            return state.get().getResult();
        });
    }

    private interface FilterState<Val> {
        FilterState<Val> next();

        default Maybe<FilterStateResulted<Val>> asResulted() {
            return Maybe.none();
        }

        default FilterStateResulted<Val> nextResulted() {
            FilterState<Val> current = this;
            MutableBox<FilterStateResulted<Val>> nullable_Result = new MutableBox<>(null);
            do {
                current = current.next();
                current.asResulted().ifSome(nullable_Result::set);
            }
            while (nullable_Result.get() == null);
            return nullable_Result.get();
        }
    }

    private interface FilterStateResulted<Val> extends FilterState<Val> {
        Maybe<Val> getResult();

        @Override
        default Maybe<FilterStateResulted<Val>> asResulted() {
            return Maybe.some(this);
        }
    }

    private record FilterState_Iter<Val>(Pred<Val> condition, Iter<Val> iter) implements FilterState<Val> {
        @Override
        public FilterState<Val> next() {
            return iter.next()
                       .<FilterState<Val>>map(val -> (
                               condition.apply(val)
                               ? new FilterStateResulted_Iter_Val<>(condition, iter, val)
                               : this))
                       .orElse(new FilterStateResulted_Done<>());
        }
    }

    private record FilterStateResulted_Iter_Val<Val>(Pred<Val> condition,
                                                     Iter<Val> iter,
                                                     Val val)
            implements FilterStateResulted<Val> {
        @Override
        public Maybe<Val> getResult() {
            return Maybe.some(val);
        }

        @Override
        public FilterState<Val> next() {
            return new FilterState_Iter<>(condition, iter);
        }
    }

    private record FilterStateResulted_Done<Val>() implements FilterStateResulted<Val> {
        @Override
        public Maybe<Val> getResult() {
            return Maybe.none();
        }

        @Override
        public FilterState<Val> next() {
            return this;
        }
    }

    public <MVal> Iter<MVal> bind(Func<Val, Iter<MVal>> action) {
        return join(map(action));
    }

    public Iter<Val> or(Iter<Val> other) {
        MutableBox<OrState<Val>> state = new MutableBox<>(new OrState_Iter_Iter<>(this, other));
        return of(() -> {
            OrStateResulted<Val> resulted = state.get().nextResulted();
            state.set(resulted);
            return resulted.getResult();
        });
    }

    private interface OrState<Val> {
        OrState<Val> next();

        default Maybe<OrStateResulted<Val>> asResulted() {
            return Maybe.none();
        }

        default OrStateResulted<Val> nextResulted() {
            MutableBox<OrState<Val>> state = new MutableBox<>(this);
            MutableBox<OrStateResulted<Val>> nullableResulted = new MutableBox<>(null);
            do {
                state.modify(OrState::next);
                state.get().asResulted().ifSome(nullableResulted::set);
            }
            while (nullableResulted.get() == null);
            return nullableResulted.get();
        }
    }

    private interface OrStateResulted<Val> extends OrState<Val> {
        Maybe<Val> getResult();

        @Override
        default Maybe<OrStateResulted<Val>> asResulted() {
            return Maybe.some(this);
        }
    }

    private record OrState_Iter_Iter<Val>(Iter<Val> iter1, Iter<Val> iter2) implements OrState<Val> {
        @Override
        public OrState<Val> next() {
            return iter1.next()
                        .<OrState<Val>>map(val -> (
                                new OrStateResulted_Iter_Iter_Val<>(iter1, iter2, val)))
                        .orElse(new OrState_Iter<>(iter2));
        }
    }

    private record OrState_Iter<Val>(Iter<Val> iter) implements OrState<Val> {
        @Override
        public OrState<Val> next() {
            return iter.next()
                       .<OrState<Val>>map(val -> (
                               new OrStateResulted_Iter_Val<>(iter, val)))
                       .orElse(new OrStateResulted_Done<>());
        }
    }

    private record OrStateResulted_Iter_Iter_Val<Val>(Iter<Val> iter1,
                                                      Iter<Val> iter2,
                                                      Val val)
            implements OrStateResulted<Val> {
        @Override
        public Maybe<Val> getResult() {
            return Maybe.some(val);
        }

        @Override
        public OrState<Val> next() {
            return new OrState_Iter_Iter<>(iter2, iter1);
        }
    }

    private record OrStateResulted_Iter_Val<Val>(Iter<Val> iter, Val val)
            implements OrStateResulted<Val> {
        @Override
        public Maybe<Val> getResult() {
            return Maybe.some(val);
        }

        @Override
        public OrState<Val> next() {
            return new OrState_Iter<>(iter);
        }
    }

    private record OrStateResulted_Done<Val>() implements OrStateResulted<Val> {
        @Override
        public Maybe<Val> getResult() {
            return Maybe.none();
        }

        @Override
        public OrState<Val> next() {
            return this;
        }
    }
}
