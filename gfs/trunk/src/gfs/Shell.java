package gfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.SynchronousQueue;

import jol.core.Runtime;
import jol.core.System;
import jol.types.basic.Tuple;
import jol.types.basic.TupleSet;
import jol.types.basic.ValueList;
import jol.types.exception.JolRuntimeException;
import jol.types.exception.UpdateException;
import jol.types.table.Table;
import jol.types.table.TableName;
import jol.types.table.Table.Callback;

public class Shell {
    private int currentMaster;
    private System system;
    private Random rand;
    private SynchronousQueue responseQueue;

    /*
     * TODO:
     *  (1) connect to an instance of JOL
     *  (2) parse command-line argument into command + arguments
     *  (3) inject the appropriate inserts into JOL; wait for the results to come back
     *  (4) return results to stdout
     */
    public static void main(String[] args) throws Exception {
        Shell shell = new Shell();
        List<String> argList = new LinkedList<String>(Arrays.asList(args));

        if (argList.size() == 0)
            shell.usage();

        String op = argList.remove(0);
        if (op.equals("append")) {
            shell.doAppend(argList);
        } else if (op.equals("read")) {
            shell.doRead(argList);
        } else if (op.equals("cat")) {
            shell.doConcatenate(argList);
        } else if (op.equals("create")) {
            shell.doCreateFile(argList, true);
        } else if (op.equals("ls")) {
            ValueList<String> list = shell.doListFiles(argList);
            java.lang.System.out.println("ls:");
            int i = 1;
            for (String file : list) {
                java.lang.System.out.println("  " + i + ". " + file);
                i++;
            }
        } else if (op.equals("rm")) {
            shell.doRemove(argList);
        } else {
            shell.usage();
        }

        shell.shutdown();
    }

    public Shell() throws JolRuntimeException, UpdateException {
        this.rand = new Random();
        this.responseQueue = new SynchronousQueue();
        this.currentMaster = 0;

        /* Identify the address of the local node */
        Conf.setSelfAddress("tcp:localhost:5501");

        this.system = Runtime.create(5501);

        this.system.install("gfs_global", ClassLoader.getSystemResource("gfs/gfs_global.olg"));
        this.system.evaluate();
        this.system.install("gfs", ClassLoader.getSystemResource("gfs/gfs.olg"));
        this.system.evaluate();

        scheduleNewMaster();
        this.system.start();
    }

    private void doAppend(List<String> args) {
        if (args.size() != 1)
            usage();

        String filename = args.get(0);
    }

    private void doRead(List<String> args) throws InterruptedException {
        if (args.size() != 1)
            usage();

        String filename = args.get(0);

        /* Ask a master node for the list of blocks */
        List<Integer> blocks = getBlockList(filename);

        /*
         * For each block, ask the master node for a list of data nodes that
         * hold the block, and then read the block's contents from those nodes.
         */
        for (Integer block : blocks) {
            List<String> locations = getBlockLocations(block);
            readFile(locations);
        }
    }

    private ValueList getBlockList(String filename) throws InterruptedException {
        final int requestId = generateId();

        // Register a callback to listen for responses
        Callback responseCallback = new Callback() {
            @Override
            public void deletion(TupleSet tuples) {}

            @Override
            public void insertion(TupleSet tuples) {
                for (Tuple t : tuples) {
                    Integer tupRequestId = (Integer) t.value(1);

                    if (tupRequestId.intValue() == requestId) {
                        Object blockList = t.value(4);
                        try {
                            responseQueue.put(blockList);
                            break;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        Table responseTbl = registerCallback(responseCallback, "response");

        ValueList blockList = (ValueList) this.responseQueue.take();
        responseTbl.unregister(responseCallback);
        return blockList;
    }

    private Table registerCallback(Callback callback, String tableName) {
        Table table = this.system.catalog().table(new TableName("gfs", tableName));
        table.register(callback);
        return table;
    }

    private List<String> getBlockLocations(Integer block) {
        return null;
    }

    private void readFile(List<String> locations) {
        ;
    }

    private void doConcatenate(List<String> args) throws UpdateException, InterruptedException {
        if (args.isEmpty())
            usage();

        for (String file : args)
            doCatFile(file);
    }

    private void scheduleNewMaster() throws UpdateException, JolRuntimeException {
        TupleSet master = new TupleSet();
        master.add(new Tuple(Conf.getSelfAddress(),
                             Conf.getMasterAddress(this.currentMaster)));
        this.system.schedule("gfs", MasterTable.TABLENAME, master, null);
        this.system.evaluate();
    }

    private void doCatFile(final String file) throws UpdateException, InterruptedException {
        final int requestId = generateId();

        // Register callback to listen for responses
        Callback responseCallback = new Callback() {
            @Override
            public void deletion(TupleSet tuples) {}

            @Override
            public void insertion(TupleSet tuples) {
                for (Tuple t : tuples) {
                    Integer tupRequestId = (Integer) t.value(1);

                    if (tupRequestId.intValue() == requestId) {
                        Boolean success = (Boolean) t.value(3);

                        if (success.booleanValue()) {
                            ValueList blocks = (ValueList) t.value(4);
                            java.lang.System.out.println("File name: " + file);
                            java.lang.System.out.println("Blocks: " + blocks.toString());
                            java.lang.System.out.println("=============");
                        } else {
                            String errMessage = (String) t.value(4);
                            java.lang.System.out.println("ERROR on \"cat\":");
                            java.lang.System.out.println("File name: " + file);
                            java.lang.System.out.println("Error message: " + errMessage);
                        }

                        try {
                            responseQueue.put("done");
                            break;
                        }
                        catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        Table responseTbl = registerCallback(responseCallback, "response");

        // Create and insert the request tuple
        TableName tblName = new TableName("gfs", "start_request");
        TupleSet req = new TupleSet(tblName);
        req.add(new Tuple(Conf.getSelfAddress(), requestId, "Cat", file));
        this.system.schedule("gfs", tblName, req, null);

        // Wait for the response
        Object obj = this.responseQueue.take();
        responseTbl.unregister(responseCallback);
    }

    private int generateId() {
        return rand.nextInt();
    }

    public void doCreateFile(List<String> args, boolean fromStdin) throws UpdateException, InterruptedException, JolRuntimeException {
        if (args.size() != 1)
            usage();

        // XXX: file content isn't used right now
        StringBuilder sb = new StringBuilder();
        if (fromStdin && false) {
            /* Read the contents of the file from stdin */
            int b;
            try {
                while ((b = java.lang.System.in.read()) != -1)
                    sb.append((char) b);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            sb.append("foo");
        }

        String filename = args.get(0);
        final int requestId = generateId();

        // Register a callback to listen for responses
        Callback responseCallback = new Callback() {
            @Override
            public void deletion(TupleSet tuples) {}

            @Override
            public void insertion(TupleSet tuples) {
                for (Tuple t : tuples) {
                    Integer tupRequestId = (Integer) t.value(1);

                    if (tupRequestId.intValue() == requestId) {
                        Boolean success = (Boolean) t.value(3);

                        if (success.booleanValue()) {
                            java.lang.System.out.println("Create succeeded.");
                        } else {
//                             String errMessage = (String) t.value(4);
                            java.lang.System.out.println("Create failed.");
//                             java.lang.System.out.println("Error message: " + errMessage);
                        }

                        try {
                            responseQueue.put(success);
                            break;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        Table responseTbl = registerCallback(responseCallback, "response");

        // Create and insert the request tuple
        TableName tblName = new TableName("gfs", "start_request");
        TupleSet req = new TupleSet(tblName);
        req.add(new Tuple(Conf.getSelfAddress(), requestId, "Create", filename));
        this.system.schedule("gfs", tblName, req, null);

        // Wait for the response
        Object obj = timedTake(this.responseQueue, Conf.getFileOpTimeout());
        responseTbl.unregister(responseCallback);
        if (obj == null) {
          // we timed out.
          java.lang.System.out.println("retrying (master indx = " + this.currentMaster + ")\n");
          doCreateFile(args, fromStdin);
        }
    }

    public ValueList<String> doListFiles(List<String> args) throws UpdateException, InterruptedException, JolRuntimeException {
        if (!args.isEmpty())
            usage();

        final int requestId = generateId();

        // Register a callback to listen for responses
        Callback responseCallback = new Callback() {
            @Override
            public void deletion(TupleSet tuples) {}

            @Override
            public void insertion(TupleSet tuples) {
                for (Tuple t : tuples) {
                    Integer tupRequestId = (Integer) t.value(1);

                    if (tupRequestId.intValue() == requestId) {
                        Object lsContent = t.value(4);
                        try {
                            responseQueue.put(lsContent);
                            break;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };

        Table responseTbl = registerCallback(responseCallback, "response");

        // Create and insert the request tuple
        TableName tblName = new TableName("gfs", "start_request");
        TupleSet req = new TupleSet(tblName);
        req.add(new Tuple(Conf.getSelfAddress(), requestId, "Ls", null));
        this.system.schedule("gfs", tblName, req, null);

        Object obj = timedTake(this.responseQueue, Conf.getListingTimeout());
        responseTbl.unregister(responseCallback);
        if (obj == null)
            return doListFiles(args);

        ValueList<String> lsContent = (ValueList<String>) obj;
        Collections.sort(lsContent);
        return lsContent;
    }

    public void doRemove(List<String> argList) throws UpdateException, InterruptedException, JolRuntimeException {
        if (argList.isEmpty())
            usage();

        for (String s : argList) {
            doRemoveFile(s);
        }
    }

    protected void doRemoveFile(final String file) throws UpdateException, InterruptedException, JolRuntimeException {
        final int requestId = generateId();

        // Register callback to listen for responses
        Callback responseCallback = new Callback() {
            @Override
            public void deletion(TupleSet tuples) {}

            @Override
            public void insertion(TupleSet tuples) {
                for (Tuple t : tuples) {
                    Integer tupRequestId = (Integer) t.value(1);

                    if (tupRequestId.intValue() == requestId) {
                        Boolean success = (Boolean) t.value(3);

                        if (success.booleanValue()) {
                            java.lang.System.out.println("Remove of file \"" + file + "\": success.");
                        } else {
//                             Object content = t.value(5);
                            java.lang.System.out.println("ERROR on \"rm\":");
                            java.lang.System.out.println("File name: " + file);
//                             java.lang.System.out.println("Error message: " + content);
                        }

                        try {
                            responseQueue.put(success);
                            break;
                        }
                        catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };
        Table responseTbl = registerCallback(responseCallback, "response");

        // Create and insert the request tuple
        TableName tblName = new TableName("gfs", "start_request");
        TupleSet req = new TupleSet(tblName);
        req.add(new Tuple(Conf.getSelfAddress(), requestId, "Rm", file));
        this.system.schedule("gfs", tblName, req, null);

        // Wait for the response
        //Object obj = this.responseQueue.take();
        Object obj = timedTake(this.responseQueue, Conf.getFileOpTimeout());
        responseTbl.unregister(responseCallback);
        if (obj == null)
           doRemoveFile(file);
    }

    private Object timedTake(SynchronousQueue q, int millis) throws InterruptedException, JolRuntimeException, UpdateException {
        JOLTimer t = new JOLTimer(millis, this.responseQueue);
        t.start();
        Object obj = this.responseQueue.take();
        t.stop();
        Object ret = null;

        /* doing this cleanup here may be ill advised.
           but I do want to do it in one place only.
           leaks?
        */
        try {
            String msg = (String) obj;
            if (msg.compareTo("timeout") == 0) {
                this.currentMaster++;
                if (this.currentMaster == Conf.getNumMasters()) {
                    java.lang.System.out.println("giving up\n");
                    java.lang.System.exit(1);
                }
                scheduleNewMaster();
            }
        } catch (ClassCastException e) {
            // fine, it's not a timeout then.
            java.lang.System.out.println("looks good\n");
            ret = obj;
        }
        return ret;
    }

    public void shutdown() {
        this.system.shutdown();
    }

    private void usage() {
        java.lang.System.err.println("Usage: java gfs.Shell op_name args");
        java.lang.System.err.println("Where op_name = {append,cat,create,ls,read,rm}");

        shutdown();
        java.lang.System.exit(0);
    }
}
