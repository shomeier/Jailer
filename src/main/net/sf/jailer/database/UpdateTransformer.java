/*
 * Copyright 2007 - 2017 the original author or authors.
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.jailer.CommandLineParser;
import net.sf.jailer.Configuration;
import net.sf.jailer.database.Session.AbstractResultSetReader;
import net.sf.jailer.database.Session.ResultSetReader;
import net.sf.jailer.datamodel.Column;
import net.sf.jailer.datamodel.Table;
import net.sf.jailer.util.CellContentConverter;
import net.sf.jailer.util.Quoting;
import net.sf.jailer.util.SqlUtil;

/**
 * A {@link ResultSetReader} that writes the read rows as UPDATE statements 
 * into the export-script.
 * 
 * @author Ralf Wisser
 */
public class UpdateTransformer extends AbstractResultSetReader {

    /**
     * The table to read from.
     */
    private final Table table;
    
    /**
     * The columns to update.
     */
    private final Set<Column> columns;
    
    /**
     * The file to write to.
     */
    private final OutputStreamWriter scriptFileWriter;
    
    /**
     * Number of columns.
     */
    private int columnCount;

    /**
     * Labels of columns.
     */
    private String[] columnLabel = null;

    /**
     * Labels of columns as comma separated list.
     */
    private String labelCSL;

    /**
     * For building compact insert-parts of upsert-statements.
     */
    private Map<String, StatementBuilder> upsertInsertStatementBuilder = new HashMap<String, StatementBuilder>();

    /**
     * Maximum length of SQL values list (for generated inserts).
     */
    private final int maxBodySize;
    
    /**
     * For quoting of column names.
     */
    private final Quoting quoting;
    
    /**
     * Current session;
     */
    private final Session session;

    /**
     * Configuration of the target DBMS.
     */
    private final Configuration targetDBMSConfiguration;

    /**
     * SQL Dialect.
     */
	private final SQLDialect currentDialect;
	
    /**
     * Constructor.
     * 
     * @param table the table to read from
     * @param scriptFileWriter the file to write to
     * @param maxBodySize maximum length of SQL values list (for generated inserts)
     * @param session the session
     * @param targetDBMSConfiguration configuration of the target DBMS
     */
    public UpdateTransformer(Table table, Set<Column> columns, OutputStreamWriter scriptFileWriter, int maxBodySize, Session session, Configuration targetDBMSConfiguration) throws SQLException {
        this.targetDBMSConfiguration = targetDBMSConfiguration;
        this.maxBodySize = maxBodySize;
        this.table = table;
        this.columns = columns;
        this.scriptFileWriter = scriptFileWriter;
        this.currentDialect = targetDBMSConfiguration.getSqlDialect();
        this.quoting = new Quoting(session);
        if (targetDBMSConfiguration != null && targetDBMSConfiguration != Configuration.forDbms(session)) {
        	if (targetDBMSConfiguration.getIdentifierQuoteString() != null) {
        		this.quoting.setIdentifierQuoteString(targetDBMSConfiguration.getIdentifierQuoteString());
        	}
        }
        this.session = session;
    }

    private Set<String> columnNamesLower = new HashSet<String>();
    
    /**
     * Reads result-set and writes into export-script.
     */
    public void readCurrentRow(ResultSet resultSet) throws SQLException {
    	if (columnLabel == null) {
            columnCount = getMetaData(resultSet).getColumnCount();
            columnLabel = new String[columnCount + 1];
            labelCSL = "";
            for (int i = 1; i <= columnCount; ++i) {
                String mdColumnLabel = quoting.quote(getMetaData(resultSet).getColumnLabel(i));
                
                columnLabel[i] = mdColumnLabel;
                if (labelCSL.length() > 0) {
                    labelCSL += ", ";
                }
                labelCSL += columnLabel[i];
            }
            for (Column column: columns) {
            	columnNamesLower.add(column.name.toLowerCase());
            }
        }
        try {
            StringBuffer valueList = new StringBuffer("");
            StringBuffer namedValues = new StringBuffer("");
            boolean f = true;
            CellContentConverter cellContentConverter = getCellContentConverter(resultSet, session, targetDBMSConfiguration);
			for (int i = 1; i <= columnCount; ++i) {
				Object content = null;
				if (columnLabel[i] == null) {
					continue;
				}
				if (content == null) {
	            	content = cellContentConverter.getObject(resultSet, i);
	                if (resultSet.wasNull()) {
	                    content = null;
	                }
				}
                if (!f) {
                	namedValues.append(", ");
                	valueList.append(", ");
                }
                f = false;
                String cVal = cellContentConverter.toSql(content);
            	valueList.append(cVal);
                namedValues.append(cVal + " " + columnLabel[i]);
            }

			if (table.primaryKey.getColumns().isEmpty()) {
            	throw new RuntimeException("Unable to merge/upsert into table \"" + table.getName() + "\".\n" +
            			"No primary key.");
            }

            Map<String, String> val = new HashMap<String, String>();
            StringBuffer valuesWONull = new StringBuffer("");
            StringBuffer columnsWONull = new StringBuffer("");
            f = true;
            for (int i = 1; i <= columnCount; ++i) {
                if (columnLabel[i] == null) {
                	continue;
                }
                Object content = cellContentConverter.getObject(resultSet, i);
                if (resultSet.wasNull()) {
                    content = null;
                }
                String cVal = cellContentConverter.toSql(content);
                if (targetDBMSConfiguration.dbms == DBMS.POSTGRESQL && (content instanceof Date || content instanceof Timestamp)) {
                	// explicit cast needed
                	cVal = "timestamp " + cVal;
                }
                if (targetDBMSConfiguration.dbms == DBMS.POSTGRESQL) {
                	// explicit cast needed
                	int mdColumnType = getMetaData(resultSet).getColumnType(i);
                	if (mdColumnType == Types.TIME) {
                		cVal = "time " + cVal;
                	}
                }
            	val.put(columnLabel[i], cVal);
                if (content != null) {
                    if (!f) {
                    	valuesWONull.append(", ");
                        columnsWONull.append(", ");
                    }
                    f = false;
                    valuesWONull.append(cVal);
                    columnsWONull.append(columnLabel[i]);
                }
            }
            
            f = true;
            StringBuffer whereForTerminator = new StringBuffer("");
            StringBuffer where = new StringBuffer("");
            StringBuffer whereWOAlias = new StringBuffer("");
            
            // assemble 'where' for sub-select and update
            for (Column pk: table.primaryKey.getColumns()) {
                if (!f) {
                    whereForTerminator.append(" and ");
                    where.append(" and ");
                    whereWOAlias.append(" and ");
                }
                f = false;
                whereForTerminator.append("T." + quoting.requote(pk.name) + "=Q." + quoting.requote(pk.name));
                String value;
                String name = quoting.quote(pk.name);
                if (val.containsKey(name)) {
                	value = val.get(name);
                } else if (val.containsKey(name.toLowerCase())) {
                	value = val.get(name.toLowerCase());
                } else {
                	value = val.get(name.toUpperCase());
                }
                where.append("T." + quoting.requote(pk.name) + "=" + value);
                whereWOAlias.append(quoting.requote(pk.name) + "=" + value);
            }

            if (currentDialect.upsertMode == UPSERT_MODE.MERGE) {
            	// MERGE INTO JL_TMP T USING (SELECT 1 c1, 2 c2 from dual) incoming 
            	// ON (T.c1 = incoming.c1) 
            	// WHEN MATCHED THEN UPDATE SET T.c2 = incoming.c2 
            	String insertHead = "MERGE INTO " + qualifiedTableName(table) + " T USING(";
                StringBuffer terminator = new StringBuffer(") Q ON(" + whereForTerminator + ") ");
                
                StringBuffer sets = new StringBuffer();
                StringBuffer tSchema = new StringBuffer();
                StringBuffer iSchema = new StringBuffer();
                for (int i = 1; i <= columnCount; ++i) {
                    if (columnLabel[i] == null) {
                    	continue;
                    }
                    if  (!isPrimaryKeyColumn(columnLabel[i])) {
	                    if (sets.length() > 0) {
	                    	sets.append(", ");
	                    }
	                    sets.append("T." + columnLabel[i] + "=Q." + columnLabel[i]);
                    }
                    if (tSchema.length() > 0) {
                    	tSchema.append(", ");
                    }
                    tSchema.append("T." + columnLabel[i]);
                    if (iSchema.length() > 0) {
                    	iSchema.append(", ");
                    }
                    iSchema.append("Q." + columnLabel[i]);
                }
                if (sets.length() > 0) {
                	terminator.append("WHEN MATCHED THEN UPDATE SET " + sets +  ";\n");
                }
            	
                StatementBuilder sb = upsertInsertStatementBuilder.get(insertHead);
                if (sb == null) {
                    sb = new StatementBuilder(maxBodySize);
                    upsertInsertStatementBuilder.put(insertHead, sb);
                }
            
                String item = "Select " + valueList + " from dual";
                if (!sb.isAppendable(insertHead, item)) {
                    writeToScriptFile(sb.build(), true);
                }
                if (sb.isEmpty()) {
                	item = "Select " + namedValues + " from dual";
                }
            	sb.append(insertHead, item, " UNION ALL ", terminator.toString());
            }
            
            if (currentDialect.upsertMode != UPSERT_MODE.MERGE) {
                StringBuffer insert = new StringBuffer("");
                insert.append("Update " + qualifiedTableName(table) + " set ");
                f = true;
                for (int i = 1; i <= columnCount; ++i) {
                    if (!columnNamesLower.contains(columnLabel[i].toLowerCase())) {
                    	continue;
                    }
                	if (isPrimaryKeyColumn(columnLabel[i])) {
                    	continue;
                    }
                    if (!f) {
                        insert.append(", ");
                    }
                    f = false;
                    insert.append(columnLabel[i] + "=" + val.get(columnLabel[i]));
                }
                if (!f) {
                	insert.append(" Where " + whereWOAlias + ";\n");
                    writeToScriptFile(insert.toString(), true);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets qualified table name.
     * 
     * @param t the table
     * @return qualified name of t
     */
    private String qualifiedTableName(Table t) {
    	String schema = t.getOriginalSchema("");
    	String mappedSchema = CommandLineParser.getInstance().getSchemaMapping().get(schema);
    	if (mappedSchema != null) {
    		schema = mappedSchema;
    	}
    	if (schema.length() == 0) {
    		return quoting.requote(t.getUnqualifiedName());
    	}
		return quoting.quote(schema) + "." + quoting.requote(t.getUnqualifiedName());
	}

	/**
     * Checks if columns is part of primary key.
     * 
     * @param column the column
     * @return <code>true</code> if column is part of primary key
     */
    private boolean isPrimaryKeyColumn(String column) {
    	for (Column c: table.primaryKey.getColumns()) {
    		if (c.name.equalsIgnoreCase(column)) {
    			return true;
    		}
    	}
		return false;
	}

	/**
     * Flushes the export-reader.
     */
    public void flush() {
        try {
            for (StatementBuilder sb: upsertInsertStatementBuilder.values()) {
                writeToScriptFile(sb.build(), true);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
	/**
     * Flushes the export-reader.
     */
    public void close() {
    	flush();
    }
    
    /**
     * Writes into script.
     */
    private void writeToScriptFile(String content, boolean wrap) throws IOException {
        synchronized (scriptFileWriter) {
        	if (wrap && targetDBMSConfiguration.dbms == DBMS.ORACLE) {
       			scriptFileWriter.write(SqlUtil.splitDMLStatement(content, 2400));
        	} else {
        		scriptFileWriter.write(content);
        	}
        }
    }
    
}