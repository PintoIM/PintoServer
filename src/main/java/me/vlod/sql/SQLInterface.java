package me.vlod.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public interface SQLInterface {
	/**
	 * Creates a new table
	 * 
	 * @param name the table name
	 * @param columns the table columns in the format ColumnName:ColumnType,
	 * the column type must be a valid SQL type and must be supported by the interface
	 * @return if the operation was successful
	 * @implSpec if the name is null return false
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean createTable(String name, LinkedHashMap<String, String> columns) throws SQLException;
	
	/**
	 * Checks if a table with the given name exists
	 * 
	 * @param name the table name
	 * @return if a table exists
	 * @implSpec if the name is null return false,
	 * if columns are empty or null assume no columns
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean doesTableExist(String name) throws SQLException;
	
	/**
	 * Deletes a table with the specified name
	 * 
	 * @param name the table name
	 * @return if the operation was successful
	 * @implSpec if the name is null return false
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean deleteTable(String name) throws SQLException;

	/**
	 * Gets all of the rows in the specified table
	 * 
	 * @param tableName the table name
	 * @return the rows values or null
	 * @implSpec if the table name is null return null
	 * @throws SQLException if an SQL error has occurred
	 */
	ArrayList<String[]> getRows(String tableName) throws SQLException;

	/**
	 * Gets rows that match the specified selector in the specified table
	 * 
	 * @param tableName the table name
	 * @param selector the selector
	 * @return the rows values or null
	 * @implSpec if the table name is null return null
	 * @throws SQLException if an SQL error has occurred
	 */
	ArrayList<String[]> getRows(String tableName, String selector) throws SQLException;
	
	/**
	 * Creates a new row in the specified table<br>
	 * Strings must be quoted, otherwise a syntax error will be thrown
	 * 
	 * @param tableName the table name
	 * @param values the values
	 * @return if the operation was successful
	 * @implSpec if the table name is null return false
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean createRow(String tableName, String[] values) throws SQLException;

	/**
	 * Changes ALL rows in the specified table<br>
	 * The columns and values must match exactly
	 * 
	 * @param tableName the table name
	 * @param columns the columns that should be affected
	 * @param values the new values
	 * @return if the operation was successful
	 * @implSpec if the table name is null return false,
	 * if the columns and values are different sizes, throw {@link IllegalArgumentException}
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean changeRows(String tableName, String[] columns, String[] values) throws SQLException;
	
	/**
	 * Changes rows that match the specified selector in the specified table<br>
	 * The columns and values must match exactly
	 * 
	 * @param tableName the table name
	 * @param selector the selector
	 * @param columns the columns that should be affected
	 * @param values the new values
	 * @return if the operation was successful
	 * @implSpec if the table name is null return false,
	 * if the columns and values are different sizes, throw {@link IllegalArgumentException}
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean changeRows(String tableName, String selector, 
			String[] columns, String[] values) throws SQLException;
	
	/**
	 * Deletes ALL rows in the specified table
	 * 
	 * @param tableName the table name
	 * @return if the operation was successful
	 * @implSpec if the table name is null return false
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean deleteRows(String tableName) throws SQLException;
	
	/**
	 * Deletes rows that match the specified selector in the specified table
	 * 
	 * @param tableName the table name
	 * @param selector the selector
	 * @return if the operation was successful
	 * @implSpec if the table name or the selector are null return false
	 * @throws SQLException if an SQL error has occurred
	 */
	boolean deleteRows(String tableName, String selector) throws SQLException;
}
