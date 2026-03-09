package sol.jara.franckie.validation.core;


import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;


sealed interface ComposedRule<T> extends Rule<T> {

    List<Rule<T>> getInnerRules();

    record ContraMap<S,T>(Function<S,T> projector, Rule<T> rule) implements Rule<S> {}

    record Guard<T>(Rule<T> precondition, Rule<T> rule) implements Rule<T> {}

    record Or<T>(Rule<T> a, Rule<T> b) implements ComposedRule<T> {
        @Override
        public List<Rule<T>> getInnerRules() {
            return List.of(a, b);
        }
    }

    record And<T>(Rule<T> a, Rule<T> b) implements ComposedRule<T> {
        @Override
        public List<Rule<T>> getInnerRules() {
            return List.of(a, b);
        }
    }

    record Not<T>(Rule<T> rule) implements ComposedRule<T> {
        @Override
        public List<Rule<T>> getInnerRules() {
            return List.of(rule);
        }
    }

    record Any<T>(List<Rule<T>> rules) implements ComposedRule<T> {
        @Override
        public List<Rule<T>> getInnerRules() {
            return rules;
        }
    }

    record AllOf<T>(List<Rule<T>> rules) implements ComposedRule<T> {
        @Override
        public List<Rule<T>> getInnerRules() {
            return rules;
        }
    }

    record WithCode<T>(
            Rule<T> rule,
            String messageCode,
            Function<Result, Map<String, String>> argumentsFunction,
            boolean abortOnFail
    ) implements ComposedRule<T> {

        @Override
        public List<Rule<T>> getInnerRules() {
            return List.of(rule);
        }
    }
}

sealed interface EdgeRule<T> extends Rule<T> {

    record Equals<T, A>(Attr<T, A> field, A value) implements EdgeRule<T> {
    }

    record EqualsAttr<T, A>(Attr<T, A> left, Attr<T, A> right) implements EdgeRule<T> {
    }

    record GreaterThan<T, A extends Comparable<? super A>>(Attr<T, A> field, A value) implements EdgeRule<T> {
        int compareTo(T value) {
            A found = this.field.getter().apply(value);
            return found.compareTo(this.value());
        }
    }

    record LessThan<T, A extends Comparable<? super A>>(Attr<T, A> field, A value) implements EdgeRule<T> {
        int compareTo(T value) {
            A found = this.field.getter().apply(value);
            return found.compareTo(this.value());
        }
    }

    record GreaterThanAttr<T, A extends Comparable<? super A>>(Attr<T, A> left, Attr<T, A> right) implements EdgeRule<T> {
        int compare(T target) {
            A l = left.getter().apply(target);
            A r = right.getter().apply(target);
            return l.compareTo(r);
        }
    }

    record LessThanAttr<T, A extends Comparable<? super A>>(Attr<T, A> left, Attr<T, A> right) implements EdgeRule<T> {
        int compare(T target) {
            A l = left.getter().apply(target);
            A r = right.getter().apply(target);
            return l.compareTo(r);
        }
    }

    record ContainsAll<T, A>(Attr<T, List<A>> superset, Attr<T, List<A>> subset) implements EdgeRule<T> {
    }

    record IsNull<T, A>(Attr<T, A> field) implements EdgeRule<T> {
    }

    record NotNull<T, A>(Attr<T, A> field) implements EdgeRule<T> {
    }

    record IsEmpty<T,A>(Attr<T,Optional<A>> field) implements EdgeRule<T> {}

    record IsEmptyCollection<T,A>(Attr<T,Collection<A>> field) implements EdgeRule<T> {}

    record In<T, A>(Attr<T, A> field, Collection<A> allowed) implements EdgeRule<T> { }

    record IsContainIn<T,A>(Attr<T, A> field, Attr<T,List<A>> allowed) implements EdgeRule<T> {}

    /**
     * Custom predicate-based validation rule
     *
     * @param <T> Target object type
     * @param <A> Attribute type being validated
     * @param field The attribute accessor
     * @param predicate The validation predicate
     * @param description Human-readable description of the validation
     */
    record Predicate<T, A>(
        Attr<T, A> field,
        java.util.function.Predicate<A> predicate,
        String description
    ) implements EdgeRule<T> {}

}

public sealed interface Rule<T> permits ComposedRule, EdgeRule, ComposedRule.ContraMap, ComposedRule.Guard {

    default <S> Rule<S> contramap(Function<S, T> projector) {
        return new ComposedRule.ContraMap<>(projector, this);
    }

    static <T, A extends Comparable<? super A>> Rule<T> gt(Attr<T, A> left, Attr<T, A> right) {
        return new EdgeRule.GreaterThanAttr<>(left, right);
    }

    static <T, A extends Comparable<? super A>> Rule<T> gt(Attr<T, A> field, A value) {
        return new EdgeRule.GreaterThan<>(field, value);
    }

    static <T, A> Rule<T> eq(Attr<T, A> field, A value) {
        return new EdgeRule.Equals<>(field, value);
    }

    static <T, A> Rule<T> eq(Attr<T, A> left, Attr<T, A> right) {
        return new EdgeRule.EqualsAttr<>(left, right);
    }

    static <T, A> Rule<T> notEq(Attr<T, A> field, A value) {
        return new ComposedRule.Not<>(new EdgeRule.Equals<>(field, value));
    }

    static <T, A> Rule<T> notEq(Attr<T, A> left, Attr<T, A> right) {
        return new ComposedRule.Not<>(new EdgeRule.EqualsAttr<>(left, right));
    }

    static <T, A extends Comparable<? super A>> Rule<T> lt(Attr<T, A> field, A value) {
        return new EdgeRule.LessThan<>(field, value);
    }


    static <T, A extends Comparable<? super A>> Rule<T> lt(Attr<T, A> left, Attr<T, A> right) {
        return new EdgeRule.LessThanAttr<>(left, right);
    }

    static <T, A> Rule<T> isNull(Attr<T, A> field) {
        return new EdgeRule.IsNull<>(field);
    }

    static <T, A> Rule<T> notNull(Attr<T, A> field) {
        return new ComposedRule.Not<>(new EdgeRule.IsNull<>(field));
    }

    static <T, A> Rule<T> isEmpty(Attr<T, Optional<A>> field) {return new EdgeRule.IsEmpty<>(field);}

    static <T, A> Rule<T> isEmptyCollection(Attr<T, Collection<A>> field) {return new EdgeRule.IsEmptyCollection<>(field);}

    static <T> Rule<T> or(Rule<T> a, Rule<T> b) {
        return new ComposedRule.Or<>(a, b);
    }

    static <T> Rule<T> guard(Rule<T> precondition, Rule<T> rule) {
        return new ComposedRule.Guard<>(precondition, rule);
    }

    static <T> Rule<T> and(Rule<T> a, Rule<T> b) {
        return new ComposedRule.And<>(a, b);
    }

    static <T> Rule<T> not(Rule<T> rule) {
        return new ComposedRule.Not<>(rule);
    }

    @SafeVarargs
    static <T> Rule<T> any(Rule<T>... rules) {
        return new ComposedRule.Any<>(List.of(rules));
    }

    @SafeVarargs
    static <T> Rule<T> allOf(Rule<T>... rules) {
        return new ComposedRule.AllOf<>(List.of(rules));
    }

    /**
     * Rule to specify that superset must contain all the elements in the subset
     * @param superset
     * @param subset
     * @return
     * @param <T>
     * @param <A>
     */
    static <T, A> Rule<T> containsAll(Attr<T, List<A>> superset,
                                      Attr<T, List<A>> subset) {
        return new EdgeRule.ContainsAll<>(superset, subset);
    }

    static <T, A> Rule<T> in(Attr<T, A> field, Collection<A> allowed) {
        return new EdgeRule.In<>(field, allowed);
    }

    @SafeVarargs
    static <T, A> Rule<T> in(Attr<T, A> field, A... allowed) {
        return new EdgeRule.In<>(field, List.of(allowed));
    }

    static <T,A> Rule<T> isContainIn(Attr<T, A> field, Attr<T,List<A>> allowed) {
        return new EdgeRule.IsContainIn<>(field, allowed);
    }

    /**
     * Creates a rule based on object-level predicate (not tied to a specific attribute).
     * This allows validation logic that requires access to multiple fields of the object.
     *
     * @param predicate The validation predicate that tests the entire object
     * @param description Human-readable description of the validation
     * @return Rule for object-level validation
     * @param <T> The type of object being validated
     */
    static <T> Rule<T> satisfies(
        java.util.function.Predicate<T> predicate,
        String description
    ) {
        Attr<T, T> identity = Attr.of("object", t -> t);
        return new EdgeRule.Predicate<>(identity, predicate, description);
    }

    /**
     * Creates a rule based on object-level predicate with default description.
     *
     * @param predicate The validation predicate that tests the entire object
     * @return Rule for object-level validation
     * @param <T> The type of object being validated
     */
    static <T> Rule<T> satisfies(java.util.function.Predicate<T> predicate) {
        return satisfies(predicate, "custom object predicate");
    }

    /**
     * This is the rule to match in order to run the guarded rule
     * @param rule the guarded rule if the current Rule is matched
     * @return
     */
    default Rule<T> guard( Rule<T> rule) {
        return new ComposedRule.Guard<>(this, rule);
    }

    default Rule<T> or(Rule<T> b) {
        return new ComposedRule.Or<>(this, b);
    }

    default Rule<T> and(Rule<T> b) {
        return new ComposedRule.And<>(this, b);
    }

    default Rule<T> onInvalid(String messageCode, Function<Result, Map<String, String>> argumentsFunction) {
        return new ComposedRule.WithCode<>(this, messageCode, argumentsFunction, false);
    }

    default Rule<T> onInvalid(String messageCode) {
        return new ComposedRule.WithCode<>(this, messageCode, Rule.defaultArguments(), false);
    }

    static Function<Result, Map<String, String>> defaultArguments() {
        return r -> {
            Map<String, String> map = new HashMap<>();
            r.left().ifPresent(v -> map.put("left", v));
            r.right().ifPresent(v -> map.put("right", v));
            return map;
        };
    }

    default Rule<T> onInvalidAndAbort(String messageCode, Function<Result, Map<String, String>> argumentsFunction) {
        return new ComposedRule.WithCode<>(this, messageCode, argumentsFunction, true);
    }

    default Rule<T> onInvalidAndAbort(String messageCode) {
        return new ComposedRule.WithCode<>(this, messageCode, Rule.defaultArguments(), true);
    }


    static <T> Result interpret(T target, EdgeRule<T> rule, List<Violation> violations) {

        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(violations, "violations");

        return switch (rule) {
            case EdgeRule.Equals<T, ?> eq -> {
                var attr = eq.field();
                var value = eq.value();
                Object found = attr.getter().apply(target);
                boolean res = (Objects.equals(found, value));

                var result =  new Result(
                        res,
                        "%s=%s".formatted(attr.name(), value),
                        "%s=%s".formatted(attr.name(), found),
                        value != null ? Optional.of(value.toString()) : Optional.empty(),
                        found != null ? Optional.of(found.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.eq".formatted(attr.name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.EqualsAttr<T, ?> eqAttr -> {
                var leftAttr = eqAttr.left();
                var rightAttr = eqAttr.right();
                Object left = leftAttr.getter().apply(target);
                Object right = rightAttr.getter().apply(target);
                boolean res = (left == null ? right == null : left.equals(right));

                String expected = "%s==%s".formatted(leftAttr.name(), rightAttr.name());
                String found = "%s=%s, %s=%s".formatted(
                        leftAttr.name(), left,
                        rightAttr.name(), right
                );

                var result =  new Result(res,
                        expected,
                        found,
                        left != null ? Optional.of(left.toString()) : Optional.empty(),
                        right != null ? Optional.of(right.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.eq.%s".formatted(leftAttr.name(), rightAttr.name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }
            case EdgeRule.GreaterThan<T, ?> gt -> {
                Comparable<?> found = gt.field().getter().apply(target);
                Comparable<?> expected = gt.value();
                int comp = gt.compareTo(target);
                boolean res = comp > 0;

                var result = new Result(
                        res,
                        "%s>%s".formatted(found, expected),
                        comp < 0 ? "%s<%s".formatted(found, expected) : "%s=%s".formatted(found, expected),
                        expected != null ? Optional.of(expected.toString()) : Optional.empty(),
                        found != null ? Optional.of(found.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.gt".formatted(gt.field().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }
            case EdgeRule.LessThan<T, ?> lt -> {
                Comparable<?> found = lt.field().getter().apply(target);
                Comparable<?> expected = lt.value();
                int comp = lt.compareTo(target);
                boolean res = comp < 0;

                var result = new Result(
                        res,
                        "%s<%s".formatted(found, expected),
                        comp > 0 ? "%s>%s".formatted(found, expected) : "%s=%s".formatted(found, expected),
                        expected != null ? Optional.of(expected.toString()) : Optional.empty(),
                        found != null ? Optional.of(found.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.lt".formatted(lt.field().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.GreaterThanAttr<T, ?> gtAttr -> {
                Comparable<?> left = (Comparable<?>) gtAttr.left().getter().apply(target);
                Comparable<?> right = (Comparable<?>) gtAttr.right().getter().apply(target);
                int comp = gtAttr.compare(target);
                boolean res = comp > 0;

                var result = new Result(
                        res,
                        "%s>%s".formatted(gtAttr.left().name(), gtAttr.right().name()),
                        "%s>%s".formatted(left, right),
                        left != null ? Optional.of(left.toString()) : Optional.empty(),
                        right != null ? Optional.of(right.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.gt.%s".formatted(gtAttr.left().name(), gtAttr.right().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.LessThanAttr<T, ?> ltAttr -> {
                Comparable<?> left = (Comparable<?>) ltAttr.left().getter().apply(target);
                Comparable<?> right = (Comparable<?>) ltAttr.right().getter().apply(target);
                int comp = ltAttr.compare(target);
                boolean res = comp < 0;

                var result = new Result(
                        res,
                        "%s<%s".formatted(ltAttr.left().name(), ltAttr.right().name()),
                        "%s<%s".formatted(left, right),
                        left != null ? Optional.of(left.toString()) : Optional.empty(),
                        right != null ? Optional.of(right.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.lt.%s".formatted(ltAttr.left().name(), ltAttr.right().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.IsNull<T, ?> isNull -> {
                Object found = isNull.field().getter().apply(target);
                boolean res = (found == null);
                var result = new Result(
                        res,
                        "%s is null".formatted(isNull.field().name()),
                        "%s=%s".formatted(isNull.field().name(), found),
                        isNull.field().name()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.isNull".formatted(isNull.field().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.NotNull<T, ?> notNull -> {
                Object found = notNull.field().getter().apply(target);
                boolean res = (found != null);
                var result = new Result(
                        res,
                        "%s is null".formatted(notNull.field().name()),
                        "%s=%s".formatted(notNull.field().name(), found),
                        notNull.field().name()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.notNull".formatted(notNull.field().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.IsEmpty<T, ?> isEmpty -> {
                Object found = isEmpty.field().getter().apply(target);
                boolean res = (found == null || found.equals(Optional.empty()));
                var result= new Result(
                        res,
                        "%s is empty".formatted(isEmpty.field().name()),
                        "%s=%s".formatted(isEmpty.field().name(), found),
                        isEmpty.field().name()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.isEmpty".formatted(isEmpty.field().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.IsEmptyCollection<T, ?> isEmptyCollection -> {
                Collection<?> found = isEmptyCollection.field().getter().apply(target);
                boolean res = (found == null || found.isEmpty());
                var result = new Result(
                        res,
                        "%s is empty".formatted(isEmptyCollection.field().name()),
                        "%s=%s".formatted(isEmptyCollection.field().name(), found),
                        isEmptyCollection.field().name()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.isEmptyCollection".formatted(isEmptyCollection.field().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }

            case EdgeRule.ContainsAll<T, ?> cAll -> {
                var superset = (List<?>) cAll.superset().getter().apply(target);
                var subset = (List<?>) cAll.subset().getter().apply(target);

                boolean ok = new HashSet<>(superset).containsAll(subset);

                String expected = "%s containsAll %s".formatted(
                        cAll.superset().name(),
                        cAll.subset().name()
                );

                String found = "%s=%s ; %s=%s".formatted(
                        cAll.superset().name(), superset,
                        cAll.subset().name(), subset
                );

                var result =  new Result(
                        ok,
                        expected,
                        found,
                        Optional.of(subset.toString()),
                        Optional.of(superset.toString())
                );

                if(!ok){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.containsAll.%s".formatted(cAll.superset().name(), cAll.subset().name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;

            }

            case EdgeRule.In<T, ?> in -> {
                var attr    = in.field();
                Object found = attr.getter().apply(target);
                Collection<?> allowed = in.allowed();

                boolean res = allowed.contains(found);

                String expected = "%s in %s".formatted(attr.name(), allowed);
                String foundStr = "%s=%s".formatted(attr.name(), found);

                var result = new Result(
                        res,
                        expected,
                        foundStr,
                        Optional.of(allowed.toString()),
                        found != null ? Optional.of(found.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.in".formatted(attr.name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;
            }
            case EdgeRule.IsContainIn<T,?> isContainIn -> {
                var requestedAttr = isContainIn.field();
                Object found = requestedAttr.getter().apply(target);

                var allowedAttr = isContainIn.allowed();
                List<?> allowed = allowedAttr.getter().apply(target);

                boolean res = allowed.contains(found);

                String expected = "%s in %s".formatted(requestedAttr.name(), allowedAttr.name());
                String foundStr = "%s=%s".formatted(requestedAttr.name(), found);

                var result = new Result(
                        res,
                        expected,
                        foundStr,
                        Optional.of(allowed.toString()),
                        found != null ? Optional.of(found.toString()) : Optional.empty()
                );

                if(!res){
                    violations.add(
                            Violation.of(
                                    "validation.field.%s.in.%s".formatted(requestedAttr.name(), allowedAttr.name()),
                                    Rule.defaultArguments().apply(result),
                                    result
                            )
                    );
                }
                yield result;

            }

            case EdgeRule.Predicate<T, ?> pred -> {
                var attr = pred.field();
                var predicate = pred.predicate();
                Object found = attr.getter().apply(target);

                @SuppressWarnings("unchecked")
                java.util.function.Predicate<Object> p =
                    (java.util.function.Predicate<Object>) predicate;

                boolean res = p.test(found);

                var result = new Result(
                    res,
                    "%s satisfies: %s".formatted(attr.name(), pred.description()),
                    "%s=%s".formatted(attr.name(), found),
                    Optional.ofNullable(pred.description()),
                    found != null ? Optional.of(found.toString()) : Optional.empty()
                );

                if (!res) {
                    violations.add(
                        Violation.of(
                            "validation.field.%s.predicate".formatted(attr.name()),
                            Rule.defaultArguments().apply(result),
                            result
                        )
                    );
                }

                yield result;
            }

        };
    }


    final class AbortValidation extends RuntimeException {
        private AbortValidation() {
            super(null, null, false, false); // no stack trace, no overhead
        }
    }


    static <T> List<Violation> validate(T target, Rule<T> rule) {
        List<Violation> violations = new ArrayList<>();
        try {
            collect(target, rule, violations);
        } catch (AbortValidation ignored) {
            // early exit: violations list is already populated
        }
        return violations;
    }


    private static<S,T> Result collectContraMapped(
            S target,
            ComposedRule.ContraMap<S,T> cm,
            List<Violation> violations
    ) {
        T projected = cm.projector().apply(target);
        return collect(projected, cm.rule(), violations);
    }

    private static <T> Result collect(T target, Rule<T> rule, List<Violation> violations) {

        return switch (rule) {

            case ComposedRule.ContraMap<?,?> cm ->
                collectContraMapped(target, (ComposedRule.ContraMap<? super T, ? extends Object>) cm, violations);

            case ComposedRule.WithCode<T> wc -> {
                int sizeBefore = violations.size();
                Result inner = collect(target, wc.rule(), violations);

                if (inner.failed()) {
                    // Remove default violations added by inner rules
                    int sizeAfter = violations.size();
                    if (sizeAfter > sizeBefore) {
                        violations.subList(sizeBefore, sizeAfter).clear();
                    }

                    // Add custom violation
                    var args = wc.argumentsFunction().apply(inner);
                    violations.add(Violation.of(
                            wc.messageCode(),
                            args,
                            inner
                    ));

                    if (wc.abortOnFail()) {
                        throw new AbortValidation();
                    }
                }
                yield inner;
            }


            case ComposedRule.Guard<T> g -> {
                int sizeBefore = violations.size();
                Result pre = collect(target, g.precondition(), violations);
                if (!pre.matched()) {
                    // precondition failed => do NOT evaluate the guarded rule
                    // Remove violations from precondition since the whole rule is skipped
                    int sizeAfter = violations.size();
                    if (sizeAfter > sizeBefore) {
                        violations.subList(sizeBefore, sizeAfter).clear();
                    }
                    yield new Result(
                            Result.Status.SKIPPED,
                            "guard(%s -> %s)".formatted(pre.expected(), "<skipped>"),
                            pre.found(),
                            pre.left(),
                            pre.right()
                    );
                }
                Result inner = collect(target, g.rule(), violations);
                yield new Result(
                        inner.status(),
                        "guard(%s -> %s)".formatted(pre.expected(), inner.expected()),
                        inner.found(),
                        inner.left().isPresent() ? inner.left() : pre.left(),
                        inner.right().isPresent() ? inner.right() : pre.right()
                );
            }


            case ComposedRule.And<T> and -> {
                Result a = collect(target, and.a(), violations);
                Result b = collect(target, and.b(), violations);
                Result.Status status;
                if (a.status() == Result.Status.FAILED || b.status() == Result.Status.FAILED) {
                    status = Result.Status.FAILED;
                } else if (a.status() == Result.Status.MATCHED && b.status() == Result.Status.MATCHED) {
                    status = Result.Status.MATCHED;
                } else {
                    status = Result.Status.SKIPPED;
                }

                yield new Result(
                        status,
                        "(%s AND %s)".formatted(a.expected(), b.expected()),
                        "(%s AND %s)".formatted(a.found(), b.found()),
                        a.left().isPresent() ? a.left() : b.left(),
                        a.right().isPresent() ? a.right() : b.right()
                );
            }

            case ComposedRule.Or<T> or -> {
                int sizeBefore = violations.size();
                Result a = collect(target, or.a(), violations);
                if (a.matched()) {
                    yield new Result(
                            Result.Status.MATCHED,
                            "(%s OR -)".formatted(a.expected()),
                            "(%s OR -)".formatted(a.found()),
                            a.left(),
                            a.right()
                    );
                } else {
                    Result b = collect(target, or.b(), violations);
                    Result.Status status;
                    if (a.status() == Result.Status.MATCHED || b.status() == Result.Status.MATCHED) {
                        status = Result.Status.MATCHED;
                    } else if (a.status() == Result.Status.FAILED && b.status() == Result.Status.FAILED) {
                        status = Result.Status.FAILED;
                    } else {
                        status = Result.Status.SKIPPED;
                    }

                    // If OR succeeded (one branch passed), remove all violations from both branches
                    if (status == Result.Status.MATCHED) {
                        int sizeAfter = violations.size();
                        if (sizeAfter > sizeBefore) {
                            violations.subList(sizeBefore, sizeAfter).clear();
                        }
                    }

                    Optional<String> left = b.left().isPresent() ? b.left() : a.left();
                    Optional<String> right = b.right().isPresent() ? b.right() : a.right();

                    yield new Result(
                            status,
                            "(%s OR %s)".formatted(a.expected(), b.expected()),
                            "(%s OR %s)".formatted(a.found(), b.found()),
                            left,
                            right
                    );
                }
            }

            case ComposedRule.Not<T> not -> {
                Result inner = collect(target, not.rule(), violations);
                Result.Status status = switch (inner.status()) {
                    case MATCHED -> Result.Status.FAILED;
                    case FAILED -> Result.Status.MATCHED;
                    case SKIPPED -> Result.Status.SKIPPED;
                };
                yield new Result(
                        status,
                        "not(%s)".formatted(inner.expected()),
                        inner.found(),
                        inner.left(),
                        inner.right()
                );
            }

            case ComposedRule.Any<T> any -> {
                var results = any.rules().stream()
                        .map(r -> collect(target, r, violations))
                        .toList();

                boolean hasMatched = results.stream().anyMatch(r -> r.status() == Result.Status.MATCHED);
                boolean hasFailed = results.stream().anyMatch(r -> r.status() == Result.Status.FAILED);
                Result.Status status = hasMatched
                        ? Result.Status.MATCHED
                        : (hasFailed ? Result.Status.FAILED : Result.Status.SKIPPED);
                String expected = "any(" + results.stream()
                        .map(Result::expected)
                        .collect(joining(" ; ")) + ")";
                String found = "any(" + results.stream()
                        .map(Result::found)
                        .collect(joining(" ; ")) + ")";

                yield new Result(status, expected, found);
            }

            case ComposedRule.AllOf<T> all -> {
                var results = all.rules().stream()
                        .map(r -> collect(target, r, violations))
                        .toList();

                boolean hasFailed = results.stream().anyMatch(r -> r.status() == Result.Status.FAILED);
                boolean allMatched = results.stream().allMatch(r -> r.status() == Result.Status.MATCHED);
                Result.Status status = hasFailed
                        ? Result.Status.FAILED
                        : (allMatched ? Result.Status.MATCHED : Result.Status.SKIPPED);
                String expected = "allOf(" + results.stream()
                        .map(Result::expected)
                        .collect(joining(" ; ")) + ")";
                String found = "allOf(" + results.stream()
                        .map(Result::found)
                        .collect(joining(" ; ")) + ")";

                yield new Result(status, expected, found);
            }


            case EdgeRule.Equals<T, ?> eq -> interpret(target, eq, violations);
            case EdgeRule.GreaterThan<T, ?> gt -> interpret(target, gt, violations);
            case EdgeRule.LessThan<T, ?> lt -> interpret(target, lt, violations);
            case EdgeRule.EqualsAttr<T, ?> ea -> interpret(target, ea, violations);
            case EdgeRule.GreaterThanAttr<T, ?> gta -> interpret(target, gta, violations);
            case EdgeRule.LessThanAttr<T, ?> lta -> interpret(target, lta, violations);
            case EdgeRule.IsNull<T, ?> isNull -> interpret(target, isNull, violations);
            case EdgeRule.NotNull<T, ?> notNull -> interpret(target, notNull, violations);
            case EdgeRule.IsEmpty<T, ?> isEmpty -> interpret(target, isEmpty, violations);
            case EdgeRule.IsEmptyCollection<T, ?> isEmptyCollection -> interpret(target, isEmptyCollection, violations);
            case EdgeRule.ContainsAll<T, ?> ca -> interpret(target, ca, violations);
            case EdgeRule.In<T, ?> in -> interpret(target, in, violations);
            case EdgeRule.IsContainIn<T,?> isContainIn -> interpret(target, isContainIn, violations);
            case EdgeRule.Predicate<T, ?> pred -> interpret(target, pred, violations);
        };
    }


    record LoggedViolation(
            String code,
            Map<String, String> arguments,
            boolean matched,
            String expected,
            String found,
            Optional<String> left,
            Optional<String> right
    ) {
    }

    record ValidationLogEntry(
            String commandType,
            String entityType,
            UUID commandId,
            List<LoggedViolation> violations
    ) { }


    static LoggedViolation toLoggedViolation(Violation v) {
        Result r = v.result();

        return new LoggedViolation(
                v.translationKey(),
                v.arguments() != null ? v.arguments() : Map.of(),
                r.matched(),
                r.expected(),
                r.found(),
                r.left(),
                r.right()
        );
    }

    static Optional<String> createValidationLogs(
            Command cmd,
            Object entity,
            List<Violation> violations
    ) {
        if (violations.isEmpty()) {
            return Optional.empty();
        } else {
            ValidationLogEntry logEntry =
                    new ValidationLogEntry(
                            cmd.getClass().getSimpleName(),
                            entity.getClass().getSimpleName(),
                            cmd.getCommandId(),
                            violations.stream()
                                    .map(Rule::toLoggedViolation)
                                    .toList()
                    );
            return Optional.of("Session command validation result: %s".formatted(JsonUtils.toPrettyJson(logEntry)));
        }
    }

    static void logValidation(
            Command cmd,
            Object entity,
            List<Violation> violations
    ) {
        createValidationLogs(cmd, entity, violations).ifPresent(System.err::println);
    }
}
