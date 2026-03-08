package jara.sol.franckie.validation.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record Attr<T, A>(String name, Function<T, A> getter) {
    public static <T, A> Attr<T, A> of(String name, Function<T, A> getter) {
        return new Attr<>(name, getter);
    }

    public Rule<T> eq(A value) {
        return new EdgeRule.Equals<>(this, value);
    }

    public Rule<T> eq(Attr<T, A> other) {
        return new EdgeRule.EqualsAttr<>(this, other);
    }

    public Rule<T> notEq(Attr<T, A> other) {
        return  Rule.not(this.eq(other));
    }

    public <X extends Comparable<X>> Rule<T> lt(X value) {
        @SuppressWarnings("unchecked")
        Attr<T, X> self = (Attr<T, X>) this;
        return new EdgeRule.LessThan<>(self, value);
    }

    public <X extends Comparable<X>> Rule<T> gt(X value) {
        @SuppressWarnings("unchecked")
        Attr<T, X> self = (Attr<T, X>) this;
        return new EdgeRule.GreaterThan<>(self, value);
    }

    public <X extends Comparable<? super X>> Rule<T> lt(Attr<T, X> other) {
        @SuppressWarnings("unchecked")
        Attr<T, X> self = (Attr<T, X>) this;
        return new EdgeRule.LessThanAttr<>(self, other);
    }

    public <X extends Comparable<? super X>> Rule<T> gt(Attr<T, X> other) {
        @SuppressWarnings("unchecked")
        Attr<T, X> self = (Attr<T, X>) this;
        return new EdgeRule.GreaterThanAttr<>(self, other);
    }

    public Rule<T> isNull() {
        return new EdgeRule.IsNull<>(this);
    }

    public Rule<T> notNull() {
        return new EdgeRule.NotNull<>(this);
    }

    public <E> Rule<T> isEmptyCollection() {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return new EdgeRule.IsEmptyCollection<>(self);
    }

    public <E> Rule<T> isNotEmptyCollection() {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return Rule.not(new EdgeRule.IsEmptyCollection<>(self));
    }

    public Rule<T> isEmpty() {
        @SuppressWarnings("unchecked")
        Attr<T, Optional<A>> self = (Attr<T, Optional<A>>) this;
        return new EdgeRule.IsEmpty<>(self);
    }

    public <E> Rule<T> isNotEmpty() {
        @SuppressWarnings("unchecked")
        Attr<T, Optional<E>> self = (Attr<T, Optional<E>>) this;
        return Rule.not(new EdgeRule.IsEmpty<>(self));
    }



    public <E> Rule<T> containsAll(Attr<T, List<E>> subset) {
        @SuppressWarnings("unchecked")
        Attr<T, List<E>> self = (Attr<T, List<E>>) this;
        return new EdgeRule.ContainsAll<>(self, subset);
    }

    public <E> Rule<T> containsAll(List<E> subsetValue) {
        @SuppressWarnings("unchecked")
        Attr<T, List<E>> self = (Attr<T, List<E>>) this;

        Attr<T, List<E>> constantSubset = Attr.of("<const>", __ -> subsetValue);

        return new EdgeRule.ContainsAll<>(self, constantSubset);
    }

    public Rule<T> in(Attr<T, A> field, Collection<A> allowed) {
        return new EdgeRule.In<>(field, allowed);
    }

    @SafeVarargs
    public final Rule<T> in( A... allowed) {
        Attr<T, A> self = (Attr<T, A>) this;
        return new EdgeRule.In<>(self, List.of(allowed));
    }
}
