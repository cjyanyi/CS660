package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private File file;
    private TupleDesc td;
    private int numPage;
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.file = f;
        this.td = td;
        this.numPage = (int) file.length() / BufferPool.getPageSize();
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        //throw new UnsupportedOperationException("implement this");
        return file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        //throw new UnsupportedOperationException("implement this");
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        //return null;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long pos = pid.pageNumber() * (long) BufferPool.getPageSize();
            if (pos < 0 || pos >= file.length()) {
                throw new IllegalArgumentException("The page doesn't exist in this file.");
            }

            raf.seek(pos);
            byte[] buf = new byte[BufferPool.getPageSize()];
            raf.read(buf);
            return new HeapPage((HeapPageId) pid, buf);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1

        // use pid to get pageNum
        // offset = pageNum * pageSize
        try {
            PageId pid= page.getId();

            RandomAccessFile rAf=new RandomAccessFile(file,"rw");
            int offset = pid.pageNumber()*BufferPool.getPageSize();
            byte[] b=new byte[BufferPool.getPageSize()];
            b=page.getPageData();
            rAf.seek(offset);
            rAf.write(b, 0, BufferPool.getPageSize());
            rAf.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return numPage;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        //return null;
        // not necessary for lab1
        ArrayList<Page> affectedPages = new ArrayList<>();
        for (int i = 0; i < numPages(); i++) {
            HeapPageId pid = new HeapPageId(getId(), i);
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
            if (page.getNumEmptySlots() != 0) {
                //call page.insertTuple
                page.insertTuple(t);
                page.markDirty(true, tid);
                affectedPages.add(page);
                break;
            }
        }
        if (affectedPages.size() == 0) {//page is full
            //create a new Page
            HeapPageId npid = new HeapPageId(getId(), numPages());
            HeapPage blankPage = new HeapPage(npid, HeapPage.createEmptyPageData());
            numPage++;
            //write page to disk
            writePage(blankPage);
            //read page from bufferpool
            HeapPage newPage = (HeapPage) Database.getBufferPool().getPage(tid, npid, Permissions.READ_WRITE);
            newPage.insertTuple(t);
            newPage.markDirty(true, tid);
            affectedPages.add(newPage);
        }
        return affectedPages;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        //return null;
        // not necessary for lab1

        // t->pageid, -> page
        ArrayList<Page> affectedPages = new ArrayList<>();
        PageId pid = t.getRecordId().getPageId();
        HeapPage affectedPage = null;
        for (int i = 0; i < numPages(); i++) {
            if (i == pid.pageNumber()) {
                affectedPage = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
                affectedPage.deleteTuple(t);
                affectedPages.add(affectedPage);
            }
        }
        if (affectedPages.size()==0) {
            throw new DbException("tuple " + t + " is not in this table");
        }
        return affectedPages;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid);
    }

    private class HeapFileIterator implements DbFileIterator {

        private int curPage = 0;
        private Iterator<Tuple> curItr = null;
        private TransactionId tid;
        private boolean open = false;

        public HeapFileIterator(TransactionId tid) {
            this.tid = tid;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            open = true;
            curPage = 0;
            if (curPage >= numPages()) {
                return;
            }
            // all getPage op must be done by BufferPool, except DbFile.readPage
            curItr = ((HeapPage) Database.getBufferPool().getPage(tid,
                    new HeapPageId(getId(), curPage), Permissions.READ_ONLY))
                    .iterator();
            advance();
        }

        private void advance() throws DbException, TransactionAbortedException {
            while (!curItr.hasNext()) {
                curPage++;
                if (curPage < numPages()) {
                    curItr = ((HeapPage) Database.getBufferPool().getPage(tid,
                            new HeapPageId(getId(), curPage),
                            Permissions.READ_ONLY)).iterator();
                } else {
                    break;
                }
            }
        }
        // pageid->page->tuple
        @Override
        public boolean hasNext() throws DbException,
                TransactionAbortedException {
            if (!open) {
                return false;
            }
            return curPage < numPages();
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException,
                NoSuchElementException {
            if (!open) {
                throw new NoSuchElementException("iterator not open.");
            }
            if (!hasNext()) {
                throw new NoSuchElementException("No more tuples.");
            }
            Tuple result = curItr.next();
            advance();
            return result;
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            if (!open) {
                throw new DbException("iterator not open yet.");
            }
            close();
            open();
        }

        @Override
        public void close() {
            curItr = null;
            curPage = 0;
            open = false;
        }


    }

}

