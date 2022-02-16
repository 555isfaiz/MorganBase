package morgan.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
	int length() default 0;

	boolean index() default false;

	boolean unique() default false;

	boolean nullable() default false;

	String defaults() default "";

	String comments() default "";
}
