package p2.core;


import java.lang.reflect.Constructor;
import java.util.Hashtable;

import p2.types.basic.Tuple;
import p2.types.basic.TupleSet;
import p2.types.element.Element;
import p2.types.exception.BadKeyException;
import p2.types.exception.UpdateException;
import p2.types.table.Key;
import p2.types.table.ObjectTable;
import p2.types.table.Schema;
import p2.types.table.Table;

public class Plumber extends ObjectTable {
	private static Long identifiers = new Long(0);
	private static String newID() {
		return "Element:" + Plumber.identifiers++;
	}
	
	private static final Key PRIMARY_KEY = new Key(0); 
	
	private static final Schema SCHEMA = 
		new Schema(new Schema.Entry("ID",       String.class),
				   new Schema.Entry("RuleID",   String.class),
				   new Schema.Entry("Name",     String.class),
				   new Schema.Entry("Type",     String.class),
				   new Schema.Entry("Object",   Element.class));
				   
	private static Plumber PLUMBER;
	
	public Plumber(Name name, Schema schema, Integer size, Number lifetime, Key key) {
		super(name, schema, key);
	}
	
	public static void initialize() {
	if (PLUMBER == null) {
		try {
			PLUMBER = (Plumber) 
			          Table.create(new Table.Name("plumber", Plumber.class.getName()), 
			        		       Plumber.SCHEMA, Plumber.PRIMARY_KEY);
		} catch (UpdateException e) {
			e.printStackTrace();
			java.lang.System.exit(0);
		}
	}
	}
	
	public Element element(String id) {
		try {
			TupleSet result = primary().lookup(key().value(id));
			assert(result.size() == 1);
			return (Element) result.iterator().next().value(schema().field("Object"));
		} catch (BadKeyException e) {
			// TODO Fatal error.
			e.printStackTrace();
		}
		return null;
	}
	
	public Element create(String ruleID, String name, String type) {
		try {
			Class<Element> definition = (Class<Element>) Class.forName(type);
			Tuple t = new Tuple(Plumber.class.getName(), newID(), ruleID, name, type, definition.newInstance());
			this.insert(t);
			return element((String) t.value(schema().field("ID")));
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UpdateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean remove(Tuple tuple) throws UpdateException {
		Element element = (Element) tuple.value(schema().field("Object"));
		element.active(false);
		return super.remove(tuple);
	}

	@Override
	public Tuple insert(Tuple tuple) throws UpdateException {
		if (primary().lookup(tuple).size() == 0) {
			try {
				Class eclass = Class.forName((String)tuple.value(schema().field("Type")));
				Constructor constructor = eclass.getConstructor(String.class, String.class);
				Element element = 
					(Element)  constructor.newInstance(tuple.value(schema().field("ID")), 
						                               tuple.value(schema().field("Name")));
				tuple.value(schema().field("Object"), element);
				return super.insert(tuple);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new UpdateException(e.toString());
			}
		}
		return null;
	}

}
