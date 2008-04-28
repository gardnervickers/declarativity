package p2.types.element;

import java.util.Vector;

import p2.types.basic.Tuple;
import p2.types.basic.TupleSet;
import p2.types.exception.ElementException;


public abstract class Operator extends Element {

	public Operator(String id, String name) {
		super(id, name);
	}
	
	public abstract TupleSet simple_action(TupleSet t);


	public void input(String key, Port port) throws ElementException {
		if (key != "0") {
			throw new ElementException("Operator elements have a single input port key = 0.");
		}
		super.input(key, port);
	}
	
	public void output(String key, Port port) throws ElementException {
		if (key != "0") {
			throw new ElementException("Operator elements have a single input port key = 0.");
		}
		super.output(key, port);
	}
	

}
