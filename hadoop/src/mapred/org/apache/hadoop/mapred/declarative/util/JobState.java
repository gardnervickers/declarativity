package org.apache.hadoop.mapred.declarative.util;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.JobStatus;
import org.apache.hadoop.mapred.declarative.Constants.TaskType;
import org.apache.hadoop.mapred.declarative.Constants;
import org.apache.hadoop.mapred.declarative.table.TaskTable;

public class JobState implements Comparable<JobState> {
	private JobID jobid;
	
	private Constants.JobState state;

	private int mapCount;
	
	private Set<TaskState> maps;
	
	private int reduceCount;
	
	private Set<TaskState> reduces;
	
	public JobState(JobID jobid, int mapCount, int reduceCount, Constants.JobState state) {
		this.jobid       = jobid;
		this.state       = state;
		this.mapCount    = mapCount;
		this.maps        = new HashSet<TaskState>();
		this.reduceCount = reduceCount;
		this.reduces     = new HashSet<TaskState>();
	}
	public JobState(JobID jobid, int mapCount, int reduceCount) {
		this(jobid, mapCount, reduceCount, Constants.JobState.PREP);
	}
	
	public JobState(JobID jobid) {
		this(jobid, 0, 0);
	}
	
	public String toString() {
		JobStatus status = status();
		return this.jobid.toString() + " - " + 
		       this.state + 
		       " MapProgress[" + status.mapProgress() + "]," +
		       " ReduceProgress[" + status.reduceProgress() + "]";
	}
	
	public int compareTo(JobState o) {
		return this.jobid.compareTo(o.jobid);
	}
	
	public JobID jobID() {
		return this.jobid;
	}
	
	public void task(TaskType type, TaskState state) {
		if (type == TaskType.MAP) {
			this.maps.add(state);
		}
		else if (type == TaskType.REDUCE) {
			this.reduces.add(state);
		}
		status();
	}
	
	public Constants.JobState state() {
		return this.state;
	}
	
	public JobStatus status() {
		float mapProgress    = 0f;
		float reduceProgress = 0f;
		
		if (this.state == Constants.JobState.RUNNING) {
			for (TaskState map : maps) {
				mapProgress += map.progress();
			}
			mapProgress = mapProgress / (float) this.mapCount;
			for (TaskState reduce : reduces) {
				reduceProgress += reduce.progress();
			}
			reduceProgress = reduceProgress / (float) this.reduceCount;
		}
		else if (this.state == Constants.JobState.SUCCEEDED) {
			mapProgress = reduceProgress = 1f;
		}
		
		if (mapProgress == 1f && reduceProgress == 1f) {
			this.state = Constants.JobState.SUCCEEDED;
		}
		
		return new JobStatus(this.jobid, mapProgress, reduceProgress, this.state.ordinal()+1);
	}

}
