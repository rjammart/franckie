package sol.jara.franckie.validation.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

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

    /**
     * Validates attribute value using custom predicate
     *
     * <p><b>⚠️ SERIALIZATION WARNING:</b> Rules created with this method are NOT serializable
     * because they contain lambda/Predicate functions. Use for runtime validation only.
     * For persistent rules, prefer built-in validators like {@link #matches(String)},
     * {@link #between(Comparable, Comparable)}, {@link #hasSize(int)}, etc.
     *
     * @param predicate Validation predicate (not serializable)
     * @param description Human-readable description (used for default error message)
     * @return Rule for this attribute
     */
    public Rule<T> satisfies(
        java.util.function.Predicate<A> predicate,
        String description
    ) {
        return new EdgeRule.Predicate<>(this, predicate, description);
    }

    /**
     * Validates attribute value using custom predicate with default description
     *
     * <p><b>⚠️ SERIALIZATION WARNING:</b> Not serializable. Use for runtime validation only.
     *
     * @param predicate Validation predicate (not serializable)
     * @return Rule for this attribute
     */
    public Rule<T> satisfies(java.util.function.Predicate<A> predicate) {
        return satisfies(predicate, "custom predicate");
    }

    /**
     * Validates string attribute matches regex pattern
     *
     * @param pattern Regular expression pattern
     * @return Rule for this attribute
     */
    public Rule<T> matches(String pattern) {
        @SuppressWarnings("unchecked")
        Attr<T, String> self = (Attr<T, String>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.matches(pattern),
            "matches /" + pattern + "/"
        );
    }

    /**
     * Validates string attribute matches compiled regex Pattern
     *
     * @param pattern Compiled regex Pattern
     * @return Rule for this attribute
     */
    public Rule<T> matches(java.util.regex.Pattern pattern) {
        @SuppressWarnings("unchecked")
        Attr<T, String> self = (Attr<T, String>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && pattern.matcher(value).matches(),
            "matches /" + pattern.pattern() + "/"
        );
    }

    /**
     * Validates value is within range [min, max] (inclusive)
     *
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return Rule for this attribute
     * @param <N> Comparable type
     */
    public <N extends Comparable<N>> Rule<T> between(N min, N max) {
        @SuppressWarnings("unchecked")
        Attr<T, N> self = (Attr<T, N>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null &&
                     value.compareTo(min) >= 0 &&
                     value.compareTo(max) <= 0,
            "between %s and %s".formatted(min, max)
        );
    }

    /**
     * Validates collection has exact size
     *
     * @param expectedSize Expected collection size
     * @return Rule for this attribute
     * @param <E> Element type
     */
    public <E> Rule<T> hasSize(int expectedSize) {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.size() == expectedSize,
            "has size " + expectedSize
        );
    }

    /**
     * Validates collection has minimum size
     *
     * @param minSize Minimum collection size
     * @return Rule for this attribute
     * @param <E> Element type
     */
    public <E> Rule<T> hasSizeAtLeast(int minSize) {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.size() >= minSize,
            "has at least %d elements".formatted(minSize)
        );
    }

    /**
     * Validates collection has maximum size
     *
     * @param maxSize Maximum collection size
     * @return Rule for this attribute
     * @param <E> Element type
     */
    public <E> Rule<T> hasSizeAtMost(int maxSize) {
        @SuppressWarnings("unchecked")
        Attr<T, Collection<E>> self = (Attr<T, Collection<E>>) this;
        return new EdgeRule.Predicate<>(
            self,
            value -> value != null && value.size() <= maxSize,
            "has at most %d elements".formatted(maxSize)
        );
    }
}
