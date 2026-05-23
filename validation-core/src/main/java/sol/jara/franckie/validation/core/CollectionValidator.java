package sol.jara.franckie.validation.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating collections of items.
 * Allows applying a Rule&lt;Item&gt; to a List&lt;Item&gt; without manual loops.
 *
 * <p>This utility enables two-pass validation patterns where:
 * <ul>
 *   <li>Pass 1: Validate each item in a collection individually (detailed violations)</li>
 *   <li>Pass 2: Validate command-level or contextual concerns</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * // Validate a list of items
 * List&lt;Violation&gt; violations = CollectionValidator.validateEach(
 *     items,
 *     ItemRules.all()
 * );
 * </pre>
 *
 * @since 0.0.1
 */
public final class CollectionValidator {

    private CollectionValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates each item in a collection using the provided rule.
     * Collects ALL violations from ALL items.
     *
     * @param items The collection to validate
     * @param itemRule The rule to apply to each item
     * @param <T> The type of items in the collection
     * @return List of all violations from all items (empty list if no violations)
     */
    public static <T> List<Violation> validateEach(
        List<T> items,
        Rule<T> itemRule
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Violation> allViolations = new ArrayList<>();
        for (T item : items) {
            allViolations.addAll(Rule.validate(item, itemRule));
        }
        return allViolations;
    }

    /**
     * Validates each item with index information for traceability.
     * Useful for knowing which item in the list failed validation.
     *
     * <p>Note: Currently this method collects violations the same way as
     * {@link #validateEach(List, Rule)}. Future enhancement could enrich
     * violations with index information.
     *
     * @param items The collection to validate
     * @param itemRule The rule to apply to each item
     * @param indexPrefix Prefix for violations (e.g., "reservations") - reserved for future use
     * @param <T> The type of items in the collection
     * @return List of all violations from all items
     */
    public static <T> List<Violation> validateEachWithIndex(
        List<T> items,
        Rule<T> itemRule,
        String indexPrefix
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Violation> allViolations = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<Violation> itemViolations = Rule.validate(items.get(i), itemRule);

            // Future enhancement: Could enrich violations with index info here
            // e.g., modify violation keys to include "[i]" prefix
            allViolations.addAll(itemViolations);
        }
        return allViolations;
    }

    /**
     * Creates a validation rule for a command's list attribute.
     * This returns a Rule that can be composed with other rules using {@code .and()} or {@code .or()}.
     *
     * <p><strong>Limitation:</strong> This method can only detect IF items are invalid (returns ONE violation),
     * not WHICH items or WHY. For detailed per-item violations, use {@link #validateEach(List, Rule)} directly.
     *
     * <p>Example usage:
     * <pre>
     * Rule&lt;Command&gt; contextRules = COMMAND_ID.notNull()
     *     .and(CollectionValidator.validateListAttr(
     *         NEW_ITEMS,
     *         ItemRules.all(),
     *         "command.items.invalid"
     *     ));
     * </pre>
     *
     * @param listAttr The attribute representing the list to validate
     * @param itemRule The rule to apply to each item
     * @param translationKey Translation key for the violation if any item is invalid
     * @param <CMD> The command/parent type containing the list
     * @param <ITEM> The type of items in the list
     * @return A Rule that validates the list attribute
     */
    public static <CMD, ITEM> Rule<CMD> validateListAttr(
        Attr<CMD, List<ITEM>> listAttr,
        Rule<ITEM> itemRule,
        String translationKey
    ) {
        return listAttr.satisfies(
            items -> {
                if (items == null || items.isEmpty()) return true;
                return validateEach(items, itemRule).isEmpty();
            },
            "One or more items are invalid"
        ).onInvalid(translationKey);
    }
}
