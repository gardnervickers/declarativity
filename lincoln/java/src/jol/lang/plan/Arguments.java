package jol.lang.plan;

import java.util.ArrayList;
import java.util.List;

public class Arguments extends ArrayList<Expression> implements Comparable<Arguments> {
	private static final long serialVersionUID = 1L;

	private Predicate predicate;
	
	public Arguments(Predicate predicate, List<Expression> arguments) {
		super(arguments.size());
		this.predicate = predicate;
		addAll(arguments);
	}
	
	@Override
	public String toString() {
		return super.toString();
	}
	
	public int compareTo(Arguments a) {
		return this.predicate.compareTo(a.predicate);
	}

}
