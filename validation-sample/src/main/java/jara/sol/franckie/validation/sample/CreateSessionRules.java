package jara.sol.franckie.validation.sample;

import jara.sol.franckie.validation.core.Attr;
import jara.sol.franckie.validation.core.Rule;

/**
 * Validation rules for CreateSessionCommand.
 * Demonstrates the contramap pattern to reuse common SessionProjection rules
 * in the CreateSessionCommand context.
 */
public final class CreateSessionRules {

    private static final Attr<CreateSessionCommand, String> STATUS =
            Attr.of("status", CreateSessionCommand::status);

    private static final Attr<CreateSessionCommand, Boolean> SESSION_NAME_ALREADY_EXISTS =
            Attr.of("sessionNameAlreadyExists", CreateSessionCommand::sessionNameAlreadyExists);

    // Context-specific rules
    public static final Rule<CreateSessionCommand> STATUS_MUST_BE_PENDING =
            STATUS.eq("PENDING");

    public static final Rule<CreateSessionCommand> NAME_MUST_BE_UNIQUE =
            SESSION_NAME_ALREADY_EXISTS.eq(false)
                    .onInvalid("session.name.must.be.unique");

    /**
     * All validation rules for creating a session.
     * Combines context-specific rules with common session rules using contramap.
     */
    public static Rule<CreateSessionCommand> all() {
        return Rule.allOf(
                STATUS_MUST_BE_PENDING,
                NAME_MUST_BE_UNIQUE,
                // Reuse all common session rules via contramap
                CommonSessionRules.all().contramap(CreateSessionCommand::projection)
        );
    }

    private CreateSessionRules() {
        // Utility class
    }
}
