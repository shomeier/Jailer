/*
 * Copyright 2007 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jailer.database;

import java.sql.SQLException;

/**
 * Wraps {@link SQLException}s and holds SQL statement.
 * 
 * @author Ralf Wisser
 */
public class SqlException extends SQLException {

	public final String message;
	public final String sqlStatement;
	private boolean insufficientPrivileges = false;
	
	public SqlException(String message, String sqlStatement, Throwable t) {
		super(message, t);
		this.message = t == null? message : t.getMessage();
		this.sqlStatement = sqlStatement;
	}

	private static final long serialVersionUID = 766715312577675914L;

	public boolean getInsufficientPrivileges() {
		return insufficientPrivileges;
	}

	public void setInsufficientPrivileges(boolean value) {
		insufficientPrivileges = value;
	}

}
