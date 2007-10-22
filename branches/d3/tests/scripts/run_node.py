#!/usr/bin/env python2
# -*- Mode: python -*-
#
# DESCRIPTION: Setup and run n chord nodes.
#
#
import getopt
import os
import sys
import time
import random
import signal
import threading

def print_usage():
    print
    print "Usage: run_node -i <IP> -p <start_port> [-l <landmark_ip:port> [-n <num_nodes>]] [-s <seed>] [-t <session_time>] main_dir output_dir"
    print "DESC:  Start some number of chord nodes on an emulab host."
    print "DESC:  If arg seed is not given then a random seed in [0, 2^32-1] is supplied ."
    print "DESC:  If a landmark node is not supplied then it is assumed to be the master."
    print "DESC:  If a landmark node exists then a number of slaves can be supplied and assigned ports [0-(num_nodes-1)]."
    print

def parse_cmdline(argv): 
    global log
    shortopts = "n:i:p:l:t:"
    flags = {"num_nodes" : 1, "seed" : random.random()*sys.maxint, "IP" : None, 
             "start_port" : None, "landmark" : None, "session" : 0.0}
    opts, args = getopt.getopt(argv[1:], shortopts)
    for o, v in opts:
        if   o == "-n": flags["num_nodes"]  = v
        elif o == "-s": flags["seed"]       = v
        elif o == "-i": flags["IP"]         = v
        elif o == "-p": flags["start_port"] = v
        elif o == "-l": flags["landmark"]   = v
        elif o == "-t": flags["session"]    = float(v)
    if args[1]: log = open(args[1]+"/setup_node.log", 'w')
    return flags, args

# e.g., runChord <loggingLevel> <seed> <myipaddr:port> [<landmark_ipaddr:port>]\n";
def run_node(main_dir, seed, ip, p, lm, out):
    output = open(out, 'a')
    print >> output, "NODEIP = %s:%s"               % (ip, p)
    print >> output, "LANDMARK = %s"                % (lm)
    print >> output, "SIMULATION TIME = %f seconds" % (time.time() - start_time)
    print >> output, "%s/runChord %s/chord2.plg NONE %d %s:%s 10 %s >> %s 2>&1" % (main_dir, main_dir, seed, ip, p, lm, out) 
    output.close()

    pid = os.fork()
    if pid == 0:
        if lm: rv = os.system(r"%s/runChord %s/chord2.plg NONE %d %s:%s 10 %s >> %s 2>&1" % (main_dir, main_dir, seed, ip, p, lm, out))
        else:  rv = os.system(r"%s/runChord %s/chord2.plg NONE %d %s:%s 0 >> %s 2>&1" % (main_dir, main_dir, seed, ip, p, out))
        print >> log, "SYSTEM CALL EXIT"
        sys.exit(0)
    return pid

def reaper(p):
    try:
        os.kill(p, signal.SIGKILL)
    except:
        print >> log, sys.exc_info()[:2]
    
if __name__ == "__main__":
    global start_time
    try:
        flags, args = parse_cmdline(sys.argv)
    except:
        print_usage()
        sys.exit(3)
    if len(args) < 1:
        print_usage()        
        sys.exit(3)

    for node in range(int(flags["num_nodes"])):
        try:
            os.remove(args[1]+"/chord_node" + str(node) + ".out")
        except: pass 

    seeds      = [int(flags["seed"]) + x for x in range(int(flags["num_nodes"]))]
    pids       = []
    start_time = time.time()
    nodes      = int(flags["num_nodes"])
    start_port = int(flags["start_port"])
    firstTime  = True

    while 1:
        for portinc in range(100):
            for node in range(nodes):
                pid = run_node(args[0], seeds[node], flags["IP"], start_port+(nodes*portinc + node), flags["landmark"], \
                               args[1]+"/chord_node" + str(node) + ".out")
                pids.append(pid)

            print >> log, "PIDS: ", pids
            print >> log, "TIME: ", time.time() - start_time
            if flags["session"]: 
                if firstTime: 
                    firstTime = False
                    threading.Timer(5.0 + (random.random() * flags["session"]), (lambda: map(reaper, pids))).start()
                else: threading.Timer(flags["session"], (lambda: map(reaper, pids))).start()

            while pids:
                log.flush()
                p, s = os.wait() 
                print >> log, "PID DONE: ", p
                pids.remove(p)
            time.sleep(5)
            if not flags["session"]: 
		sys.exit(0)
