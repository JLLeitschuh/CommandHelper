package com.laytonsmith.PureUtilities.Common.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation, when tagging a constructor, indicates that ALL subclasses must
 * include a constructor with the same parameter signature. This check is enforced
 * at compile time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface MustIncludeConstructor {
	
}
