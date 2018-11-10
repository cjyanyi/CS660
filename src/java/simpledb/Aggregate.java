package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    private DbIterator child;
    private DbIterator results;
    private int agField;
    private int gbField;
    private Aggregator.Op agOp;
    private final Aggregator aggr;
    Type gbFieldType, aggFieldType;

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     * 
     * 
     * @param child
     *            The DbIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(DbIterator child, int afield, int gfield, Aggregator.Op aop) {
	// some code goes here
        this.child = child;
        agField = afield;
        gbField = gfield;
        agOp = aop;

        if (gbField != Aggregator.NO_GROUPING) {
            gbFieldType = child.getTupleDesc().getFieldType(gbField);
        }
        aggFieldType = child.getTupleDesc().getFieldType(agField);
        if (aggFieldType == Type.INT_TYPE) {
            aggr = new IntegerAggregator(gbField, gbFieldType, agField, agOp);
        } else {
            aggr = new StringAggregator(gbField, gbFieldType, agField, agOp);
        }
        results = null;
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
	    // some code goes here
	    return gbField;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples If not, return
     *         null;
     * */
    public String groupFieldName() {
	    // some code goes here
        if (gbField != Aggregator.NO_GROUPING) {
             return child.getTupleDesc().getFieldName(gbField);
        }
        return null;
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
	    // some code goes here
	    return agField;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
	    // some code goes here
	    // return null;
        if (gbField != Aggregator.NO_GROUPING) {
            return getTupleDesc().getFieldName(1);
        }
        return getTupleDesc().getFieldName(0);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
	    // some code goes here
	    return agOp;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
	return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
	    TransactionAbortedException {
	    // some code goes here
        super.open();
        child.open();
        while (child.hasNext()){
            aggr.mergeTupleIntoGroup(child.next());
        }
        child.close();
        results = aggr.iterator();
        results.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate, If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
	    // some code goes here
	    if(results.hasNext()){
	        return results.next();
        }
        return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
	// some code goes here
        child.rewind();
        results.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
	// some code goes here
	//return null;
        Type[] fieldType;
        String[] fieldName;

        if(gbField == Aggregator.NO_GROUPING){
            fieldType = new Type[1];
            fieldName = new String[1];

            fieldType[0] = child.getTupleDesc().getFieldType(agField);
            fieldName[0] = agOp.toString() + "(" + child.getTupleDesc().getFieldName(agField) + ")";

            return new TupleDesc(fieldType, fieldName);
        } else {
            fieldType = new Type[2];
            fieldName = new String[2];

            fieldType[0] = child.getTupleDesc().getFieldType(gbField);
            fieldName[0] = child.getTupleDesc().getFieldName(gbField);

            fieldType[1] = child.getTupleDesc().getFieldType(agField);
            fieldName[1] = agOp.toString() + "(" + child.getTupleDesc().getFieldName(agField) + ")";
            return new TupleDesc(fieldType, fieldName);
        }
    }

    public void close() {
	// some code goes here
        super.close();
        child.close();
        results.close();
    }

    @Override
    public DbIterator[] getChildren() {
	// some code goes here
        return new DbIterator[] { this.child };
    }

    @Override
    public void setChildren(DbIterator[] children) {
	// some code goes here
        if (this.child!=children[0]) {
            this.child = children[0];
        }
    }
    
}
