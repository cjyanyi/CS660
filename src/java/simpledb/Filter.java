package simpledb;

import java.util.*;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {

    private static final long serialVersionUID = 1L;
    private Predicate pred;
    private DbIterator iter;
    private ArrayList<Tuple> tuplepassed;
    private Iterator<Tuple> tupleIterator;

    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     * 
     * @param p
     *            The predicate to filter tuples with
     * @param child
     *            The child operator
     */
    public Filter(Predicate p, DbIterator child) {
        // some code goes here
        pred = p;
        iter = child;
        tuplepassed = new ArrayList<>();
        //tupleIterator = tuplepassed.iterator();
    }

    public Predicate getPredicate() {
        // some code goes here
        return pred;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return iter.getTupleDesc();
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        super.open();
        iter.open();

        while (iter.hasNext()){
            Tuple cur = iter.next();
            if (pred.filter(cur))
                tuplepassed.add(cur);
        }

        tupleIterator = tuplepassed.iterator();
    }

    public void close() {
        // some code goes here
        super.close();
        iter.close();
        tuplepassed = null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        iter.rewind();
        tuplepassed.clear();

        while (iter.hasNext()) {
            Tuple cur = iter.next();
            if (pred.filter(cur))
                tuplepassed.add(cur);
        }

        tupleIterator = tuplepassed.iterator();
    }

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     * 
     * @return The next tuple that passes the filter, or null if there are no
     *         more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // some code goes here
        //Iterator<Tuple> it = tuplepassed.iterator();
        if(tupleIterator.hasNext())
            return tupleIterator.next();

        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        // return null;
        return new DbIterator[]{this.iter};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        if (this.iter != children[0]) {
            this.iter = children[0];
        }
    }

}
