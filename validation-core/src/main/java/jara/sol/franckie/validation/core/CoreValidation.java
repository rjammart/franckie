package jara.sol.franckie.validation.core;

import java.util.List;

public class CoreValidation {

    public static class CoreValidators {

        public static boolean decideOn(List<Violation> violations) {
            if(violations.isEmpty()) {
                return true;
                // TODO: Could use some refactoring - consider extracting validation decision logic
            } else {
                throw new ValidationException(violations);
            }
        }
    }
}
