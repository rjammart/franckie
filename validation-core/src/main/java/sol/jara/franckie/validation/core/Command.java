package sol.jara.franckie.validation.core;

import java.util.UUID;

/**
 * Base interface for commands that can be validated.
 */
public interface Command {
    /**
     * Returns the unique identifier for this command.
     * @return the command ID
     */
    UUID getCommandId();
}
