package simpledb;

import java.util.*;


/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbfield;
    private Type gbfieldType;
    private int agfield;
    private Op op;
    private HashMap<String, Integer> gbIdxToCount; //(gbfield_val_toString,  count)

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("op must be COUNT");
        }

        this.gbfield = gbfield;
        this.gbfieldType = gbfieldtype;
        this.agfield = afield;
        this.op = what;
        gbIdxToCount = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        // key
        String key = "NoGrouping";
        if (!(gbfield == Aggregator.NO_GROUPING) && gbfieldType == Type.INT_TYPE) {
            key = Integer.toString(((IntField)tup.getField(gbfield)).getValue());
        } else if (!(gbfield == Aggregator.NO_GROUPING)) {
            key = ((StringField)tup.getField(gbfield)).getValue();
        }

        // val
        if(!gbIdxToCount.containsKey(key)){
            gbIdxToCount.put(key,1);
        } else {
            int count = gbIdxToCount.get(key);
            gbIdxToCount.replace(key,count+1);
        }
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        // throw new UnsupportedOperationException("please implement me for lab3");
        return new DbIterator() {
            private Iterator iter = null;

            @Override
            public void open() throws DbException, TransactionAbortedException {
                iter = gbIdxToCount.keySet().iterator();
            }

            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
                if (iter == null)
                    throw new IllegalStateException("Operator not yet open");

                return iter.hasNext();
            }

            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if (iter == null)
                    throw new IllegalStateException("Operator not yet open");

                if (!iter.hasNext()) {
                    throw new NoSuchElementException();
                }

                String key = (String) iter.next();
                IntField val = new IntField(gbIdxToCount.get(key));
                // construct a Tuple
                TupleDesc td = getTupleDesc();
                Tuple tp = new Tuple(td);

                if (gbfield == Aggregator.NO_GROUPING) {
                    tp.setField(0, val);
                } else if (gbfieldType == Type.INT_TYPE) {
                    IntField bgFieldValue = new IntField(Integer.parseInt(key));
                    tp.setField(0, bgFieldValue);
                    tp.setField(1, val);
                } else {
                    StringField bgFieldValue = new StringField(key, key.length());
                    tp.setField(0, bgFieldValue);
                    tp.setField(1, val);
                }

                return tp;
            }

            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                close();
                open();
            }

            @Override
            public TupleDesc getTupleDesc() {
                //return null;
                if (gbfield == Aggregator.NO_GROUPING) {
                    return new TupleDesc(new Type[]{Type.INT_TYPE});
                }
                return new TupleDesc(new Type[]{gbfieldType, Type.INT_TYPE});

            }

            @Override
            public void close() {
                iter = null;
            }
        };
    }

}
