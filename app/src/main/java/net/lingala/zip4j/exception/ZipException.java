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

package net.lingala.zip4j.exception;

public class ZipException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private int code = -1;
	
	public ZipException() {
	}
	
	public ZipException(String msg) {
		super(msg);
	}
	
	public ZipException(String message, Throwable cause) {
        super(message, cause);
    }
	
	public ZipException(String msg, int code) {
		super(msg);
		this.code = code;
	}
	
	public ZipException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }
	
	public ZipException(Throwable cause) {
		super(cause);
	}
	
	public ZipException(Throwable cause, int code) {
		super(cause);
		this.code = code; 
	}
	
	public int getCode() {
		return code;
	}
	
}
