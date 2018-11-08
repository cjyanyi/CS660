package simpledb;

import java.util.*;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements DbIterator {

    private static final long serialVersionUID = 1L;
    private TransactionId tid;
    private int tableid;
    private String tableAlias;

    private DbFileIterator itr;
    //private int curPage = 0;
    //private int numPage = 0;
    //private Iterator<Tuple> curItr = null;
    //private boolean open = false;

    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid
     *            The transaction this scan is running as a part of.
     * @param tableid
     *            the table to scan.
     * @param tableAlias
     *            the alias of this table (needed by the parser); the returned
     *            tupleDesc should have fields with name tableAlias.fieldName
     *            (note: this class is not responsible for handling a case where
     *            tableAlias or fieldName are null. It shouldn't crash if they
     *            are, but the resulting name can be null.fieldName,
     *            tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        // some code goes here
        this.tid = tid;
        this.tableid = tableid;
        this.tableAlias = tableAlias;
        this.itr = Database.getCatalog().getDatabaseFile(tableid).iterator(tid);
    }

    /**
     * @return
     *       return the table name of the table the operator scans. This should
     *       be the actual name of the table in the catalog of the database
     * */
    public String getTableName() {
        return Database.getCatalog().getTableName(tableid);
    }

    /**
     * @return Return the alias of the table this operator scans.
     * */
    public String getAlias()
    {
        // some code goes here
        return tableAlias;
    }

    /**
     * Reset the tableid, and tableAlias of this operator.
     * @param tableid
     *            the table to scan.
     * @param tableAlias
     *            the alias of this table (needed by the parser); the returned
     *            tupleDesc should have fields with name tableAlias.fieldName
     *            (note: this class is not responsible for handling a case where
     *            tableAlias or fieldName are null. It shouldn't crash if they
     *            are, but the resulting name can be null.fieldName,
     *            tableAlias.null, or null.null).
     */
    public void reset(int tableid, String tableAlias) {
        // some code goes here
        this.tableAlias = tableAlias;
        this.tableid = tableid;
    }

    public SeqScan(TransactionId tid, int tableid) {
        this(tid, tableid, Database.getCatalog().getTableName(tableid));
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        itr.open();
        /*
        open = true;
        curPage = 0;
        numPage = ((HeapFile) Database.getCatalog().getDatabaseFile(tableid)).numPages();

        if (curPage >= numPage) {
            return;
        }
        curItr = ((HeapPage) Database.getBufferPool().getPage(tid,
                new HeapPageId(tableid, curPage), Permissions.READ_ONLY))
                .iterator();
        advance();*/
    }

    /*private void advance() throws DbException, TransactionAbortedException {
        while (!curItr.hasNext()) {
            curPage++;
            if (curPage < numPage) {
                curItr = ((HeapPage) Database.getBufferPool().getPage(tid,
                        new HeapPageId(tableid, curPage),
                        Permissions.READ_ONLY)).iterator();
            } else {
                break;
            }
        }
    }*/




    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name.  The alias and name should be separated with a "." character
     * (e.g., "alias.fieldName").
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     *         prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        TupleDesc td = Database.getCatalog().getTupleDesc(tableid);

        Type[] typeArr = new Type[td.numFields()];
        String[] nameArr = new String[td.numFields()];

        int i = 0;
        for (Iterator<TupleDesc.TDItem> iter = td.iterator(); iter.hasNext();) {
            TupleDesc.TDItem item = iter.next();
            typeArr[i] = item.fieldType;
            nameArr[i] = tableAlias + "." + item.fieldName;
            i++;
        }

        return new TupleDesc(typeArr, nameArr);
    }

    public boolean hasNext() throws TransactionAbortedException, DbException {
        // some code goes here
        return itr.hasNext();
        /*
        if (!open) {
            return false;
        }
        return curPage < numPage;
        */
    }

    public Tuple next() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        // some code goes here
        return itr.next();
        /*
        if (!open) {
            throw new NoSuchElementException("iterator not open.");
        }
        if (!hasNext()) {
            throw new NoSuchElementException("No more tuples.");
        }
        Tuple result = curItr.next();
        advance();
        return result;
        */
    }

    public void close() {
        // some code goes here
        itr.close();
        /*
        curItr = null;
        curPage = 0;
        open = false;
        numPage = 0;*/
    }

    public void rewind() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        itr.rewind();
    }
}
