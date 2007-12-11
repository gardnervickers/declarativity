#
# * This file is distributed under the terms in the attached LICENSE file.
# * If you do not find this file, copies can be found by writing to:
# * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300,
# * Berkeley, CA, 94704.  Attention:  Intel License Inquiry.
# * Or
# * UC Berkeley EECS Computer Science Division, 387 Soda Hall #1776,
# * Berkeley, CA,  94707. Attention: P2 Group.
#
########### Description ###########
#
#
# Given script runs aggStar.olg test and checks the test output
#
# Assumption - program is running at localhost:10000
#
#Expected output - (the order of the tuples can vary and E is a random number)
#	##Print[SendAction: RULE rule_0]:  [once(localhost:10000)]
#	##Print[SendAction: RULE rule_1]:  [twice(localhost:10000)]
#	##Print[SendAction: RULE rule_1]:  [twice(localhost:10000)]
#
####################################

#!/usr/bin/python
import os
import time
import threading
import re
import getopt
import subprocess
import sys


# Usage function
def usage():
        print """
                namelessPeriodics.py -E <planner path> -B <unitTest olg dir path> -T <time in seconds>

                -E              planner path
                -B              unitTest olg dir path
		-T		time (secs) for test to run
                -h              prints usage message
        """

# Function to parse the output file and check whether the output matches the expected value
def script_output(stdout):
       	lines=[]
	whole_output = ""
        for line in stdout.readlines():
		whole_output += line
		p = re.compile('^[#][#]Print.*$',re.VERBOSE|re.DOTALL) 
		if(p.match(line)):
			lines.append(line.rstrip())
	
	lines.sort()
	i = 1
	for line in lines:
		if i == 1:
			p = re.compile(r"""
				(^[#][#]Print\[SendAction: \s* RULE \s* rule_0\]: \s* \[once\(localhost:10000\)\])
                        	""", re.VERBOSE)
		elif i == 2:
			p = re.compile(r"""
				(^[#][#]Print\[SendAction: \s* RULE \s* rule_1\]: \s* \[twice\(localhost:10000\)\])
                                """, re.VERBOSE)
		elif i == 3:
			p = re.compile(r"""
				(^[#][#]Print\[SendAction: \s* RULE \s* rule_1\]: \s* \[twice\(localhost:10000\)\])
                                """, re.VERBOSE)
		else:
			i = i + 1
			break
	
		flag = p.match(line)
        	if flag:
        		i = i+1
       	 	else:
                	result = 0
                	break
	
	if i >4 or i <4:
		print "Test failed"
		print "Port 10000 output:"
		print whole_output
	else:
		print "Test passed"
		

#Function to kill the child after a set time
def kill_pid(stdout, pid):
        #print "killing child"
        os.kill(pid, 3)
        #print "program killed"
        script_output(stdout)


opt, arg = getopt.getopt(sys.argv[1:], 'B:E:T:h')

for key,val in opt:
        if key=='-B':
                olg_path = val
        elif key == '-E':
                executable_path = val
        elif key == '-T':
                time_interval = val
	elif key == '-h':
                usage()
                sys.exit(0)
try:
        args=[executable_path , '-o', os.path.join(olg_path, 'namelessPeriodics.olg'), '2>&1']
        #print args
	p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, close_fds=True)
except OSError, e:
        #print "Execution failed"
        print e
        sys.exit(0)

#print p.pid

if os.getpid() != p.pid:
        t = threading.Timer(int(time_interval), kill_pid, [p.stdout, p.pid])
        t.start()
