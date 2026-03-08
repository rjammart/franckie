package jara.sol.franckie.validation.sample;

import jara.sol.franckie.validation.core.Command;

import java.util.UUID;

/**
 * Command to create a new session.
 * Contains the session projection and additional context for validation.
 */
public record CreateSessionCommand(
        UUID commandId,
        SessionProjection projection,
        boolean sessionNameAlreadyExists,
        String status
) implements Command {
    @Override
    public UUID getCommandId() {
        return commandId;
    }
}
