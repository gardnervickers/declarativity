package jol.types.table;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

import jol.lang.plan.Predicate;
import jol.types.basic.Tuple;
import jol.types.basic.TupleSet;
import jol.types.basic.TypeList;
import jol.types.exception.P2RuntimeException;
import jol.types.exception.UpdateException;
import jol.types.function.Aggregate;
import jol.core.Runtime;

/**
 * A table aggregation.
 * 
 *  Table aggregations maintain all tuples inserted into the table and
 *  remove those tuples on deletion. However, the tuples that are made
 *  visible to the outside world are the aggregate values generated 
 *  by set of resident tuples. A table aggregation is created if a
 *  predicate refering to the table contains an aggregation. The
 *  aggregate function is registered with this table object. The
 *  aggregate function determines the value of the aggregate value.
 *  Deletions to this table may cause new tuples to appear. For instance
 *  if the aggregation function is a min and the tuple containing the
 *  min value is deleted then a new tuple could appear with a different
 *  (greater) min valued aggregate. The semantics of table aggregations
 *  are handled by the {@link jol.core.Driver.Flusher#insert(Tuple)}
 *  method. 
 *  
 *  TYPE: Aggregations type can be either materialized {@link Table.Type#TABLE} or
 *  event {@link Table.Type#EVENT}. An event type aggregation performs
 *  its aggregation only on the set of tuples passed into the {@link #insert(Tuple)} method.
 *  A materialized type aggregation maintains aggregate values over a stored set of
 *  tuples. Storage insert/delete semantics follow that of {@link jol.types.table.BasicTable}.
 *  
 *  PRIMARY KEY: The primary key of an Aggregate table is the GroupBy columns identified
 *  by the predicate containing the aggregation.
 */
public class Aggregation extends Table {
	
	/** Stores base tuples in aggregate functions. */
	private Hashtable<Tuple, Aggregate> baseTuples;
	
	/** Stores aggregate values derived from base tuples and aggregate functions. */
	private TupleSet aggregateTuples;
	
	/** The aggregate attribute */
	private jol.lang.plan.Aggregate aggregate;
	
	/** The primary key. */
	protected Index primary;
	
	/** The secondary indices. */
	protected Hashtable<Key, Index> secondary;
	
	/**
	 * Create a new Aggregation table.
	 * @param context The runtime context.
	 * @param predicate The predicate containing the GroupBy/Aggregation
	 * @param type The type of aggregation.
	 */
	public Aggregation(Runtime context, Predicate predicate, Table.Type type) {
		super(predicate.name(), type, key(predicate), types(predicate));
		this.baseTuples = new Hashtable<Tuple, Aggregate>();
		this.aggregateTuples = new TupleSet(name());
		
		for (jol.lang.plan.Expression arg : predicate) {
			if (arg instanceof jol.lang.plan.Aggregate) {
				this.aggregate = (jol.lang.plan.Aggregate) arg;
				break;
			}
		}
		
		if (type == Table.Type.TABLE) {
			this.primary = new HashIndex(context, this, key, Index.Type.PRIMARY);
			this.secondary = new Hashtable<Key, Index>();
		}
	}
	
	/**
	 * The aggregate variable taken from the predicate.
	 * @return The aggregate variable object.
	 */
	public jol.lang.plan.Aggregate variable() {
		return this.aggregate;
	}
	
	/**
	 * Determines the key based on the predicate.
	 * @param predicate The predicate defining the aggregate variable.
	 * @return A key the contains the GroupBy columns.
	 */
	private static Key key(Predicate predicate) {
		List<Integer> key = new ArrayList<Integer>();
		for (jol.lang.plan.Expression arg : predicate) {
			if (!(arg instanceof jol.lang.plan.Aggregate)) {
				key.add(arg.position());
			}
		}
		return new Key(key);
	}
	
	/**
	 * Extract the type of each attribute from the predicate.
	 * @param predicate The predicate containing the aggregate variable.
	 * @return An ordered list of types.
	 */
	private static TypeList types(Predicate predicate) {
		TypeList types = new TypeList();
		for (jol.lang.plan.Expression arg : predicate) {
			types.add(arg.type());
		}
		return types;
	}
	
	@Override
	public TupleSet tuples() {
		return this.aggregateTuples.clone();
	}
	
	/**
	 * Returns the (base) set of tuples contained in this table.
	 * @return The set of tuples that exist in this table after
	 * all insert/delete calls.
	 */
	private TupleSet values() {
		TupleSet values = new TupleSet(name());
		for (Aggregate value : baseTuples.values()) {
			values.add(value.result());
		}
		return values;
	}
	
	@Override
	/**
	 * The semantics of this method is somewhat different than that of 
	 * regular tables. Insertions are applied as usual. However, deletions 
	 * are applied during this operation, which may generate new insertions that
	 * become part of the delta set. 
	 */
	public TupleSet insert(TupleSet insertions, TupleSet deletions) throws UpdateException {
		if (deletions.size() > 0) {
			TupleSet intersection = deletions.clone();
			intersection.retainAll(insertions);
		
			insertions.removeAll(intersection);
			deletions.removeAll(intersection);
			TupleSet delta = delete(deletions);
			deletions.clear();
			deletions.addAll(delta);
		}
		
		for (Tuple tuple : insertions) {
			Tuple key = key().project(tuple);
			if (!baseTuples.containsKey(key)) {
				baseTuples.put(key, Aggregate.function(this.aggregate));
			}
			try {
				baseTuples.get(key).insert(tuple);
			} catch (P2RuntimeException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		TupleSet delta = values();
		delta.removeAll(tuples());  // Only those newly inserted tuples remain.
		delta.removeAll(deletions);
		
		if (type() == Table.Type.EVENT) {
			this.baseTuples.clear();
			this.aggregateTuples.clear();
			return delta;
		}
		insertions = super.insert(delta, deletions);
		return insertions;
	}
	
	@Override
	public boolean insert(Tuple tuple) throws UpdateException {
		return this.aggregateTuples.add(tuple);
	}
	
	@Override
	/** Should only be called from within this Class.  */
	public TupleSet delete(TupleSet deletions) throws UpdateException {
		if (type() == Table.Type.EVENT) {
			throw new UpdateException("Aggregation table " + name() + " is an event table!");
		}
		
		for (Tuple tuple : deletions) {
			Tuple key = key().project(tuple);
			if (this.baseTuples.containsKey(key)) {
				try {
					this.baseTuples.get(key).delete(tuple);
					if (this.baseTuples.get(key).tuples().size() == 0) {
						this.baseTuples.remove(key);
					}
				} catch (P2RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
		
		TupleSet delta = new TupleSet(name());
		delta.addAll(tuples());
		
		delta.removeAll(values());  // removed = tuples that don't exist in after.
		return super.delete(delta); // signal indices that we've removed these tuples.
	}
	
	@Override
	public boolean delete(Tuple tuple) throws UpdateException {
		return this.aggregateTuples.remove(tuple);
	}


	@Override
	public Integer cardinality() {
		return this.aggregateTuples.size();
	}

	@Override
	public Index primary() {
		return this.primary;
	}
	
	@Override
	public Hashtable<Key, Index> secondary() {
		return this.secondary;
	}

}
