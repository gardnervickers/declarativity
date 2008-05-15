package p2.lang.plan;

import java.util.Set;

import p2.types.basic.Schema;
import p2.types.basic.Tuple;
import p2.types.basic.TupleSet;
import p2.types.basic.TypeList;
import p2.types.exception.UpdateException;
import p2.types.operator.Operator;
import p2.types.table.Key;
import p2.types.table.ObjectTable;

public class Selection extends Term {
	
	public static class SelectionTable extends ObjectTable {
		public static final Key PRIMARY_KEY = new Key(0,1,2);
		
		public enum Field {PROGRAM, RULE, POSITION, OBJECT};
		public static final Class[] SCHEMA = {
			String.class,   // Program name
			String.class,   // Rule name
			Integer.class,  // Term position
			Selection.class // Selection object
		};

		public SelectionTable() {
			super("selection", PRIMARY_KEY, new TypeList(SCHEMA));
		}
		
		@Override
		protected boolean insert(Tuple tuple) throws UpdateException {
			Selection object = (Selection) tuple.value(Field.OBJECT.ordinal());
			if (object == null) {
				throw new UpdateException("Selection object null!");
			}
			object.program  = (String) tuple.value(Field.PROGRAM.ordinal());
			object.rule     = (String) tuple.value(Field.RULE.ordinal());
			object.position = (Integer) tuple.value(Field.POSITION.ordinal());
			return super.insert(tuple);
		}
	}
	
	private Boolean predicate;
	
	public Selection(Boolean predicate) {
		super();
		this.predicate = predicate;
		assert(predicate.type() == java.lang.Boolean.class);
	}
	
	@Override
	public String toString() {
		return predicate.toString();
	}

	@Override
	public Set<Variable> requires() {
		return predicate.variables();
	}
	
	public Boolean predicate() {
		return this.predicate;
	}

	@Override
	public Operator operator() {
		return new p2.types.operator.Selection(this);
	}

	@Override
	public void set(String program, String rule, Integer position) {
		Tuple tuple = new Tuple(Program.selection.name(), program, rule, position, this);
		try {
			Program.selection.force(tuple);
		} catch (UpdateException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
