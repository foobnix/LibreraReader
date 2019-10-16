/*
* Copyright 2010 Srikanth Reddy Lingala  
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, 
* software distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package net.lingala.zip4j.progress;

import net.lingala.zip4j.exception.ZipException;

/**
 * If Zip4j is set to run in thread mode, this class helps retrieve current progress
 *
 */
public class ProgressMonitor {
	
	private int state;
	private long totalWork;
	private long workCompleted;
	private int percentDone;
	private int currentOperation;
	private String fileName;
	private int result;
	private Throwable exception;
	private boolean cancelAllTasks;
	private boolean pause;
	
	//Progress monitor States
    public static final int STATE_READY = 0;
    public static final int STATE_BUSY = 1;
    
    //Progress monitor result codes
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_WORKING = 1;
    public static final int RESULT_ERROR = 2;
    public static final int RESULT_CANCELLED = 3;
    
    //Operation Types
    public static final int OPERATION_NONE = -1;
    public static final int OPERATION_ADD = 0;
    public static final int OPERATION_EXTRACT = 1;
    public static final int OPERATION_REMOVE = 2;
    public static final int OPERATION_CALC_CRC = 3;
    public static final int OPERATION_MERGE = 4;
	
	public ProgressMonitor() {
		reset();
		percentDone = 0;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public long getTotalWork() {
		return totalWork;
	}

	public void setTotalWork(long totalWork) {
		this.totalWork = totalWork;
	}

	public long getWorkCompleted() {
		return workCompleted;
	}

	public void updateWorkCompleted(long workCompleted) {
		this.workCompleted += workCompleted;
		
		if (totalWork > 0) {
			percentDone = (int)((this.workCompleted*100/totalWork));
			if (percentDone > 100) {
				percentDone = 100;
			}
		}
		while (pause) {
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				//Do nothing
			}
		}
	}

	public int getPercentDone() {
		return percentDone;
	}

	public void setPercentDone(int percentDone) {
		this.percentDone = percentDone;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getCurrentOperation() {
		return currentOperation;
	}

	public void setCurrentOperation(int currentOperation) {
		this.currentOperation = currentOperation;
	}

	public Throwable getException() {
		return exception;
	}

	public void setException(Throwable exception) {
		this.exception = exception;
	}
	
	public void endProgressMonitorSuccess() throws ZipException {
		reset();
		result = ProgressMonitor.RESULT_SUCCESS;
	}
	
	public void endProgressMonitorError(Throwable e) throws ZipException {
		reset();
		result = ProgressMonitor.RESULT_ERROR;
		exception = e;
	}
	
	public void reset() {
		currentOperation = OPERATION_NONE;
		state = STATE_READY;
		fileName = null;
		totalWork = 0;
		workCompleted = 0;
		percentDone = 0;
	}
	
	public void fullReset() {
		reset();
		exception = null;
		result = RESULT_SUCCESS;
	}

	public boolean isCancelAllTasks() {
		return cancelAllTasks;
	}

	public void cancelAllTasks() {
		this.cancelAllTasks = true;
	}

	public boolean isPause() {
		return pause;
	}

	public void setPause(boolean pause) {
		this.pause = pause;
	}
}
