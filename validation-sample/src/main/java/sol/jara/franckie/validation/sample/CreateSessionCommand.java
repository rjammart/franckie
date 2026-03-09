package sol.jara.franckie.validation.sample;

import sol.jara.franckie.validation.core.Command;
import sol.jara.franckie.validation.core.annotation.AttrName;
import sol.jara.franckie.validation.core.annotation.ValidationProjection;

import java.util.UUID;

/**
 * Command to create a new session.
 * Contains the session projection and additional context for validation.
 */
@ValidationProjection
public record CreateSessionCommand(
        UUID commandId,
        SessionProjection projection,
        boolean sessionNameAlreadyExists,
        //Test to rename the Attr
        @AttrName("CREATE_SESSION_STATUS")
        String status
) implements Command {
    @Override
    public UUID getCommandId() {
        return commandId;
    }
}
