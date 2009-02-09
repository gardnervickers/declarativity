package bfs.test;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import jol.types.exception.JolRuntimeException;
import jol.types.exception.UpdateException;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import bfs.Conf;
import bfs.DataNode;
import bfs.Master;
import bfs.Shell;

public class TestCommon {
    protected List<Master> masters;
    protected List<DataNode> datanodes;
    protected Shell shell;

    @Before
    public void doNothing() {
    }

    @After
    public void shutdown() {
        for (Master m : this.masters) {
            m.stop();
        }
        if (this.datanodes != null) {
            for (DataNode d : this.datanodes) {
                d.shutdown();
            }
        }
        System.out.println("shutdown complete\n");
    }

    protected Boolean shellLs(String... list) throws JolRuntimeException, UpdateException {
        Shell shell = new Shell();
        Boolean ret = findInLs(shell, list);
        shell.shutdown();
        return ret;
    }

    protected int shellLsCnt() throws JolRuntimeException, UpdateException {
        Shell shell = new Shell();
        int ret = lsCnt(shell);
        shell.shutdown();
        return ret;
    }


    protected void killMaster(int index) {
        this.masters.get(index).stop();
    }

    protected void shellCreate(String name) throws JolRuntimeException, UpdateException {
        Shell shell = new Shell();
        createFile(shell, name);
        shell.shutdown();
    }

    protected void shellRm(String name) throws JolRuntimeException, UpdateException {
        Shell shell = new Shell();
        rmFile(shell, name);
        shell.shutdown();
    }

    protected void assertTrue(Boolean b) {
        safeAssert("", b);
    }
    protected void safeAssert(boolean b) {
        safeAssert("", new Boolean(b));
    }

    protected void safeAssert(String s, boolean b) {
        safeAssert(s, new Boolean(b));
    }
    protected void safeAssert(String m, Boolean b) {
        if (!b) {
            System.out.println("Failed Assertion: " + m);            
            shutdown();
        }
        Assert.assertTrue(m, b);
    }

    protected int lsCnt(Shell shell) throws JolRuntimeException, UpdateException {
        List<String> list = lsFile(shell);
        return list.size();
    }

    protected Boolean findInLs(Shell shell, String... files) throws JolRuntimeException,
            UpdateException {
        List<String> list = lsFile(shell);

        // obviously not an efficient way to do this.
        for (String item : files) {
            if (!list.contains(item))
                return false;

        }
        return true;
    }

    protected void stopMany() {
        for (Master sys : this.masters) {
            sys.stop();
        }
        for (DataNode d : this.datanodes) {
            d.shutdown();
        }
    }

    protected void startMany(String... args) throws JolRuntimeException, UpdateException {
        this.masters = new LinkedList<Master>();

        Conf.setNewMasterList(args);

        for (int i = 0; i < Conf.getNumMasters(); i++) {
            Master m = new Master(i);
            m.start();
            this.masters.add(m);
        }
    }
    
    protected void cleanup(String dir) {
        File file = new File(dir);
        if (file.exists()) {
            try{
                File chunks = new File(file, "chunks");
                for (File f : chunks.listFiles()) {
                    f.delete();
                }
                File checksums = new File(file, "checksums");
                for (File f : checksums.listFiles()) {
                    f.delete();
                }
                chunks.delete();
                checksums.delete();
                file.delete();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void prepare(String dir) {
        cleanup(dir);
        new File(dir).mkdir();
        new File(dir + File.separator + "chunks").mkdir();
        new File(dir + File.separator + "checksums").mkdir();
    }

    protected void startManyDataNodes(String... args) throws JolRuntimeException,
            UpdateException {
        this.datanodes = new LinkedList<DataNode>();

        Conf.setNewDataNodeList(args.length);

        assert (args.length == Conf.getNumDataNodes());

        for (int i = 0; (i < args.length); i++) {
            prepare(args[i]);
            
            DataNode d = new DataNode(i, args[i]);
            System.out.println("new DATANODE " + d.getPort());
            d.start();
            this.datanodes.add(d);
        }
    }

    protected List<String> lsFile(Shell shell) throws UpdateException,
            JolRuntimeException {
        List<String> argList = new LinkedList<String>();
        return shell.doListFiles(argList);
    }

    protected void appendFile(Shell shell, String name, InputStream s)
            throws UpdateException {
        List<String> argList = new LinkedList<String>();
        argList.add(name);
        shell.doAppend(argList, s);
    }

    protected void createFile(Shell shell, String name) throws UpdateException,
            JolRuntimeException {
        List<String> argList = new LinkedList<String>();
        argList.add(name);
        shell.doCreateFile(argList, false);
    }

    protected void rmFile(Shell shell, String name) throws UpdateException,
            JolRuntimeException {
        List<String> argList = new LinkedList<String>();
        argList.add(name);
        shell.doRemove(argList);
    }
    /*
     * public static void main(String[] args) throws Exception { TestC t = new
     * BFSMMTest(); t.test1(); t.test2(); t.test3(); }
     */
}
