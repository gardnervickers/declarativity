package jol.types.basic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A tuple is an ordered list of values. 
 * Tuple values must implement the {#link Serializable}
 * interface in order to ship tuples to remote locations.
 */
public class Tuple implements Iterable<Object>, Serializable {
	private static final long serialVersionUID = 1L;

	/** An ordered list of tuple values. */
	protected List<Object> values;

	/** A tuple refcount. */
	transient protected long refCount;

	/** Cached hash code value */
	transient protected int hashCache;

	/** Is the cached hash code value up-to-date? */
	transient protected boolean hashCacheValid;

	transient protected boolean frozen;

	/**
	 * Create a new tuple.
	 * @param values The values that make up the tuple.
	 */
	public Tuple(Object... values) {
		initialize();
		this.values = new ArrayList<Object>();
		for (Object value : values) {
			this.values.add(value);
		}
	}

	public Object[] toArray() {
		return values.toArray();
	}
	
	/**
	 * Create a new tuple.
	 * @param values The values that make up the tuple.
	 */
	public Tuple(List<Object> values) {
		initialize();
		this.values = new ArrayList<Object>(values);
	}

	/**
	 * Read tuple from byte array.
	 * @param b Should be generated by toBytes(), not by serialization
	 * @throws IOException
	 */
	public Tuple(byte[] b) throws IOException {
		initialize();
		fromBytes(b);
	}

	private final static int NULL = 0;
	private final static int OBJECT = 1;
	private final static int STRING = 2;
	private final static int INT = 3;
	private final static int LONG = 4;
	private final static int SHORT = 5;
	private final static int BOOLEAN = 6;
	private final static int CHAR = 7;
	private final static int BYTE = 8;
	private final static int FLOAT = 9;
	private final static int DOUBLE = 10;

	private boolean warned = false;

	public byte[] toBytes() throws IOException {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(ret);
		out.writeShort(values.size());
		for (Object o : values) {
			if (o == null) {
				out.writeByte(NULL);
			} else if (o instanceof String) {
				out.writeByte(STRING);
				out.writeUTF((String)o);
			} else if (o instanceof Integer) {
				out.writeByte(INT);
				out.writeInt((Integer)o);
			} else if (o instanceof Long) {
				out.writeByte(LONG);
				out.writeLong((Long)o);
			} else if (o instanceof Short) {
				out.writeByte(SHORT);
				out.writeShort((Short)o);
			} else if (o instanceof Boolean) {
				out.writeByte(BOOLEAN);
				out.writeBoolean((Boolean)o);
			} else if (o instanceof Character) {
				out.writeByte(CHAR);
				out.writeChar((Character)o);
			} else if (o instanceof Byte) {
				out.writeByte(BYTE);
				out.writeByte((Byte)o);
			} else if (o instanceof Float) {
				out.writeByte(FLOAT);
				out.writeFloat((Float)o);
			} else if (o instanceof Double) {
				out.writeByte(DOUBLE);
				out.writeDouble((Double)o);
			} else {
				if (!warned) {
					System.out.println("sending non-primitive: " + o.getClass().toString());
					warned = true;
				}
				out.writeByte(OBJECT);
				ByteArrayOutputStream subret = new ByteArrayOutputStream();
				ObjectOutputStream oout = new ObjectOutputStream(subret);
				oout.writeObject(o);
				oout.close();
				byte[] bytes = subret.toByteArray();
				out.writeInt(bytes.length);
				out.write(bytes);
			}
		}
		out.close();
		return ret.toByteArray();
	}

	public void fromBytes(byte[] bytes) throws IOException {
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
		short size = in.readShort();
		values = new ArrayList<Object>(size);
		for (int i = 0; i < size; i++) {
			int type = in.readByte();
			if (type == NULL) {
				values.add(null);
			} else if (type == STRING) {
				values.add(in.readUTF());
			} else if (type == INT) {
				values.add(in.readInt());
			} else if (type == LONG) {
				values.add(in.readLong());
			} else if (type == SHORT) {
				values.add(in.readShort());
			} else if (type == BOOLEAN) {
				values.add(in.readBoolean());
			} else if (type == CHAR) {
				values.add(in.readChar());
			} else if (type == BYTE) {
				values.add(in.readByte());
			} else if (type == FLOAT) {
				values.add(in.readFloat());
			} else if (type == DOUBLE) {
				values.add(in.readDouble());
			} else if (type == OBJECT) {
				int len = in.readInt();
				byte[] obytes = new byte[len];
				in.readFully(obytes);
				ObjectInputStream oin = new ObjectInputStream(
										  new ByteArrayInputStream(obytes));

				try {
					values.add(oin.readObject());
				} catch (ClassNotFoundException e) {
					throw new IOException("Couldn't deserialize object in column " + i +
										  " of tuple (partial value is: " + toString() + ")");
				}
			} else {
				throw new IOException("Can't read type " + type + ".");
			}
		}
	}
	private void writeObject(ObjectOutputStream out) throws IOException {
		// Use serialization routines that are optimized for single tuples.
		// (This causes the network to use these routines, which doesn't
		// do much for performance, but helps test these routines.)
		byte[] bytes = toBytes();
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	private void readObject(ObjectInputStream in) throws IOException {
		// Custom tuple serializer
		byte[] bytes = new byte[in.readInt()];
		in.readFully(bytes);
		fromBytes(bytes);

		// We need to manually restore transient fields
		initialize();
	}

	@Override
	public Tuple clone() {
		Tuple copy    = new Tuple(this.values);
		copy.refCount = this.refCount;
		return copy;
	}
//	@Override
//	public Tuple clone() { throw new UnsupportedOperationException(); }

    /**
     * Initialize the transient fields of an empty tuple, or one that has been
     * freshly deserialized.
     */
	private void initialize() {
		this.frozen         = false;
	    this.hashCacheValid = false;
		this.refCount       = 1;
	}

	public boolean frozen() {
		return this.frozen;
	}

	public void freeze() {
		this.frozen = true;
	}

	/**
	 * Append the tuple value.
	 * @param value The tuple value.
	 */
	public void append(Object value) throws RuntimeException {
		if (frozen()) {
			throw new RuntimeException("Operation invalid on frozen tuple!");
		}
		else {
			this.values.add(value);
			this.hashCacheValid = false;
		}
	}

	@Override
	public String toString() {
		String value = "<";
		if (values.size() > 0) {
			value += values.get(0);
			for (int i = 1; i < values.size(); i++) {
				Object element = values.get(i);
				value += ", " + (element == null ? "null" : element.toString());
			}
		}
		value += ">";
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tuple) {
			Tuple t = (Tuple) obj;
			if (size() != t.size()) return false;
			for (int i = 0; i < size(); i++) {
				Object me    = this.values.get(i) == null ? "null" : this.values.get(i);
				Object other = t.values.get(i) == null ? "null" : t.values.get(i);
				if (!me.equals(other))
				    return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
	    /* If necessary, recompute the cached hash code */
	    if (!this.hashCacheValid)
	        recomputeHashCache();

	    return this.hashCache;
	}

    private void recomputeHashCache() {
        /*
         * This hash function is based on the advice in "Effective Java" by J.
         * Bloch, p. 38-39 (Item 8).
         */
        int h = 37;
        for (Object v : this.values)
            h = (h * 31) + (v == null ? 0 : v.hashCode());

        this.hashCache = h;
        this.hashCacheValid = true;
    }

	/** The number of attributes in this tuple. */
	public int size() {
		return this.values.size();
	}

	/**
	 * The value at the indicated field position. Field
	 * positions are zero-based.
	 * @param field The field position.
	 */
	public Object value(int field) {
		return this.values.get(field);
	}

	/**
	 * Set the refcount of this tuple.
	 * @param value The refcount value.
	 */
	public void refCount(long value) {
		this.refCount = value;
	}

	public long refCountInc() {
		this.refCount++;
		return this.refCount;
	}

	public long refCountInc(long value) {
		this.refCount += value;
		return this.refCount;
	}

	public long refCountDec() {
		this.refCount--;
		return this.refCount;
	}

	public long refCountDec(long value) {
		this.refCount -= value;
		return this.refCount;
	}

	/**
	 * Get the refcount of this tuple.
	 * @return The refcount.
	 */
	public long refCount() {
		return this.refCount;
	}

	public Iterator<Object> iterator() {
		return this.values.iterator();
	}
}
