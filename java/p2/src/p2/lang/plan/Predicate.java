package p2.lang.plan;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import p2.lang.plan.Selection.SelectionTable.Field;
import p2.types.basic.Schema;
import p2.types.basic.Tuple;
import p2.types.basic.TupleSet;
import p2.types.basic.TypeList;
import p2.types.exception.PlannerException;
import p2.types.exception.UpdateException;
import p2.types.operator.AntiScanJoin;
import p2.types.operator.Operator;
import p2.types.operator.ScanJoin;
import p2.types.table.Key;
import p2.types.table.ObjectTable;
import p2.types.table.TableName;
import p2.types.table.Table;

public class Predicate extends Term implements Iterable<Expression> {
	public enum Field{PROGRAM, RULE, POSITION, EVENT, OBJECT};
	public static class PredicateTable extends ObjectTable {
		public static final Key PRIMARY_KEY = new Key(0,1,2);
		
		public static final Class[] SCHEMA =  {
			String.class,     // program name
			String.class,     // rule name
			Integer.class,    // position
			String.class,     // Event
			Predicate.class   // predicate object
		};

		public PredicateTable() {
			super(new TableName(GLOBALSCOPE, "predicate"), PRIMARY_KEY, new TypeList(SCHEMA));
		}
		
		@Override
		protected boolean insert(Tuple tuple) throws UpdateException {
			Predicate object = (Predicate) tuple.value(Field.OBJECT.ordinal());
			if (object == null) {
				throw new UpdateException("Predicate object null");
			}
			object.program   = (String) tuple.value(Field.PROGRAM.ordinal());
			object.rule      = (String) tuple.value(Field.RULE.ordinal());
			object.position  = (Integer) tuple.value(Field.POSITION.ordinal());
			return super.insert(tuple);
		}
		
		@Override
		protected boolean delete(Tuple tuple) throws UpdateException {
			return super.delete(tuple);
		}
	}
	
	private boolean notin;
	
	private TableName name;
	
	private Table.Event event;
	
	private Arguments arguments;
	
	private Schema schema;
	
	public Predicate(boolean notin, TableName name, Table.Event event, List<Expression> arguments) {
		super();
		this.notin = notin;
		this.name = name;
		this.event = event;
		this.arguments = new Arguments(this, arguments);
	}
	
	public Schema schema() {
		return schema;
	}
	
	public boolean notin() {
		return this.notin;
	}
	
	public Table.Event event() {
		return this.event;
	}
	
	void event(Table.Event event) {
		this.event = event;
	}
	
	public TableName name() {
		return this.name;
	}
	
	public boolean containsAggregation() {
		for (Expression e : arguments) {
			if (e instanceof Aggregate) {
				return true;
			}
		}
		return false;
	}

	/**
	 * An iterator over the predicate arguments.
	 */
	public Iterator<Expression> iterator() {
		return this.arguments.iterator();
	}
	
	public Expression argument(Integer i) {
		return this.arguments.get(i);
	}
	
	public int arguments() {
		return this.arguments.size();
	}
	
	@Override
	public String toString() {
		assert(schema.size() == arguments.size());
		String value = (notin ? "notin " : "") + name + "(";
		if (arguments.size() == 0) {
			return value + ")";
		}
		value += arguments.get(0).toString();
		for (int i = 1; i < arguments.size(); i++) {
			value += ", " + arguments.get(i);
		}
		return value + ")";
	}

	@Override
	public Set<Variable> requires() {
		Set<Variable> variables = new HashSet<Variable>();
		for (Expression arg : arguments) {
			if (!(arg instanceof Variable)) {
				variables.addAll(arg.variables());
			}
		}
		return variables;
	}

	@Override
	public Operator operator() {
		// TODO Add code that searches for an index join
		
		if (notin) {
			return new AntiScanJoin(this);
		}
		return new ScanJoin(this);
	}
	
	@Override
	public void set(String program, String rule, Integer position) throws UpdateException {
		Program.predicate.force(new Tuple(program, rule, position, event.toString(), this));
		
		this.schema = new Schema(name());
		for (Expression arg : arguments) {
			if (arg instanceof Variable) {
				this.schema.append((Variable) arg);
			}
			else {
				this.schema.append(new DontCare(arg.type()));
			}
		}
	}
	
}
