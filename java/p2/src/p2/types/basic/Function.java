package p2.types.basic;


/**
 * A function is evaluated on a tuple. It's return
 * type can be anything in general. If the return type
 * is to be added to a tuple then it must be a subtype
 * of Comparable.
 * @param <Type> value of return type.
 */
public interface Function<Type> {

	/**
	 * Evaluate the function on the given tuple.
	 * @param tuple  The tuple argument.
	 * @return The object value of the function evaluation.
	 */
	public Type eval(Tuple tuple);
	
}
