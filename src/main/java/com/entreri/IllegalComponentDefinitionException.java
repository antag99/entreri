package com.entreri;

/**
 * IllegalComponentDefinitionException is an exception thrown if a Component
 * implementation does not follow the class hierarchy or field rules defined in
 * {@link Component}.
 * 
 * @author Michael Ludwig
 */
public class IllegalComponentDefinitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Create an exception that specifies the leaf-level class in a Component
     * type hierarchy has some problem with its definition
     * 
     * @param type The leaf, concrete type
     * @param problem A generic error message to be tacked to the end of the
     *            final error message
     */
    public IllegalComponentDefinitionException(Class<? extends Component> type, String problem) {
        super("Component type has an invalid class hierarchy: " + type + ", error: " + problem);
    }
}
