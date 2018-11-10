package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {

    private static final long serialVersionUID = 1L;

    private JoinPredicate joinPred;
    private DbIterator liter;
    private DbIterator riter;
    private TupleDesc td;

    private TupleIterator joinResults;
    // MySql buffer size
    private static  final int Block = 131072;
    //private ArrayList<Tuple> tuplesPassed;
    //private Iterator<Tuple> tupleIter;

    /**
     * Constructor. Accepts to children to join and the predicate to join them
     * on
     * 
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    public Join(JoinPredicate p, DbIterator child1, DbIterator child2) {
        // some code goes here
        joinPred = p;
        liter = child1;
        riter = child2;

        int length1 = child1.getTupleDesc().numFields();
        int length2 = child2.getTupleDesc().numFields();
        Type[] types = new Type[length1 + length2];
        String[] names = new String[types.length];
        //merge TupleDesc
        td = TupleDesc.merge(child1.getTupleDesc(), child2.getTupleDesc());

        //tuplesPassed = new ArrayList<>();
    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return joinPred;
    }

    /**
     * @return
     *       the field name of join field1. Should be quantified by
     *       alias or table name.
     * */
    public String getJoinField1Name() {
        // some code goes here
        return liter.getTupleDesc().getFieldName(joinPred.getField1());
    }

    /**
     * @return
     *       the field name of join field2. Should be quantified by
     *       alias or table name.
     * */
    public String getJoinField2Name() {
        // some code goes here
        return riter.getTupleDesc().getFieldName(joinPred.getField2());
    }

    /**
     * @see simpledb.TupleDesc#merge(TupleDesc, TupleDesc) for possible
     *      implementation logic.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    @Deprecated
    private void navieJoin () throws TransactionAbortedException, DbException{
        while (liter.hasNext()){
            Tuple l = liter.next();
            if (!riter.hasNext()) {   // the condition of while makes opIterator1.hasNext() = true
                riter.rewind();
            }
            while (riter.hasNext()){
                Tuple r = riter.next();
                if (joinPred.filter(l,r)) {
                    //tuplesPassed.add(mergeTuples(liter.getTupleDesc().numFields(),l,r));
                }
            }
        }
    }

    private TupleIterator blockNestedLoopJoin() throws DbException, TransactionAbortedException {
        LinkedList<Tuple> tuples = new LinkedList<>();
        int blockSize = Block / liter.getTupleDesc().getSize(); // num of tuples in BlockMemory
        Tuple[] cacheBlock = new Tuple[blockSize];
        int index = 0;
        int length1 = liter.getTupleDesc().numFields();
        liter.rewind();
        while (liter.hasNext()) {
            Tuple left = liter.next();
            cacheBlock[index++] = left;
            if (index >= cacheBlock.length) {//如果缓冲区满了，就先处理缓存中的tuple
                riter.rewind();
                while (riter.hasNext()) {
                    Tuple right = riter.next();
                    for (Tuple cacheLeft : cacheBlock) {
                        if (joinPred.filter(cacheLeft, right)) {//如果符合条件就合并来自两个表的tuple作为一条结果
                            Tuple result = mergeTuples(length1, cacheLeft, right);
                            tuples.add(result);
                        }
                    }
                }
                Arrays.fill(cacheBlock, null);//清空，给下一次使用
                index = 0;
            }
        }
        if (index > 0 && index < cacheBlock.length) {//处理缓冲区中剩下的tuple
            riter.rewind();
            while (riter.hasNext()) {
                Tuple right = riter.next();
                for (Tuple cacheLeft : cacheBlock) {
                    //
                    if (cacheLeft == null) break;
                    if (joinPred.filter(cacheLeft, right)) {
                        Tuple result = mergeTuples(length1, cacheLeft, right);
                        tuples.add(result);
                    }
                }
            }
        }
        return new TupleIterator(getTupleDesc(), tuples);
    }

    private Tuple mergeTuples(int length1, Tuple left, Tuple right) {
        Tuple result = new Tuple(td);
        for (int i = 0; i < length1; i++) {
            result.setField(i, left.getField(i));
        }
        for (int i = 0; i < riter.getTupleDesc().numFields(); i++) {
            result.setField(i + length1, right.getField(i));
        }
        return result;
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        super.open();
        liter.open();
        riter.open();

        joinResults = blockNestedLoopJoin();
        joinResults.open();
    }

    public void close() {
        // some code goes here
        super.close();
        liter.close();
        riter.close();
        joinResults.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        liter.rewind();
        riter.rewind();
        joinResults.rewind();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if(joinResults.hasNext())
            return joinResults.next();
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        return new DbIterator[] {liter,riter};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        liter = children[0];
        riter = children[1];
    }

}
