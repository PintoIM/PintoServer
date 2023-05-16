package me.vlod.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * SQLite implementation of {@link SQLInterface}
 */
public class SQLiteInterface implements SQLInterface {
	private Connection connection;
	private DatabaseMetaData dbMetaData;
	
	public SQLiteInterface(String filePath) throws SQLException {
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + filePath);
		this.dbMetaData = this.connection.getMetaData();
	}
	
	private int getRowCount(String tableName) throws SQLException {
		int rowCount = 0;
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("SELECT * FROM %s;", tableName));
		ResultSet query = ps.executeQuery();
		
        while (query.next()) {
        	rowCount++;
        }

        query.close();
		return rowCount;
	}
	
	private int getRowCount(String tableName, String selector) throws SQLException {
		int rowCount = 0;
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("SELECT * FROM %s WHERE %s;", tableName, selector));
		ResultSet query = ps.executeQuery();
		
        while (query.next()) {
        	rowCount++;
        }
		
        query.close();
		return rowCount;
	}

	@Override
	public boolean createTable(String name, LinkedHashMap<String, String> columns) throws SQLException {
		if (name == null || this.doesTableExist(name)) return false;
		
		String columnsExported = "";
		for (String columnLabel : columns.keySet()) {
			String columnValue = columns.get(columnLabel);
			columnsExported += String.format("%s %s, ", columnLabel, columnValue);
		}
		columnsExported = columnsExported.substring(0, columnsExported.length() - 2);

		PreparedStatement ps = this.connection.prepareStatement(
				String.format("CREATE TABLE %s (%s);", name, columnsExported));
		return ps.execute();
	}

	@Override
	public boolean doesTableExist(String name) throws SQLException {
		if (name == null) return false;
		ResultSet tables = this.dbMetaData.getTables(null, null, name, null);
		boolean query = tables.next();
		tables.close();
		return query;
	}

	@Override
	public boolean deleteTable(String name) throws SQLException {
		if (name == null || !this.doesTableExist(name)) return false;
		
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("DROP TABLE %s;", name));
		ps.executeUpdate();
		
		return true;
	}

	@Override
	public ArrayList<String[]> getRows(String tableName) throws SQLException {
		if (tableName == null || !this.doesTableExist(tableName)) return null;

		ArrayList<String[]> rows = new ArrayList<String[]>();
		int amountOfRows = this.getRowCount(tableName);
		
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("SELECT * FROM %s;", tableName));
		ResultSet query = ps.executeQuery();
		ResultSetMetaData queryMetaData = query.getMetaData();

		if (amountOfRows > 0) {
			int rowsProcessed = 0;
			do {
		    	String[] values = new String[queryMetaData.getColumnCount()];
		    	
				for (int i = 1; i < values.length + 1; i++) {
					values[i - 1] = String.valueOf(query.getObject(i));
				}
				
				rows.add(values);
				rowsProcessed++;
			} while (query.next() && rowsProcessed < amountOfRows);
		}

		query.close();
		return rows;
	}

	@Override
	public ArrayList<String[]> getRows(String tableName, String selector) throws SQLException {
		if (tableName == null || !this.doesTableExist(tableName)) return null;

		ArrayList<String[]> rows = new ArrayList<String[]>();
		int amountOfRows = this.getRowCount(tableName, selector);
		
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("SELECT * FROM %s WHERE %s;", tableName, selector));
		ResultSet query = ps.executeQuery();
		ResultSetMetaData queryMetaData = query.getMetaData();

		if (amountOfRows > 0) {
			int rowsProcessed = 0;
			do {
		    	String[] values = new String[queryMetaData.getColumnCount()];
		    	
				for (int i = 1; i < values.length + 1; i++) {
					values[i - 1] = String.valueOf(query.getObject(i));
				}
				
				rows.add(values);
				rowsProcessed++;
			} while (query.next() && rowsProcessed < amountOfRows);
		}

		query.close();
		return rows;
	}
	
	@Override
	public boolean createRow(String tableName, String[] values) throws SQLException {
		if (tableName == null || !this.doesTableExist(tableName)) return false;

		PreparedStatement ps = this.connection.prepareStatement(
				String.format("INSERT INTO %s VALUES (%s);", tableName, String.join(", ", values)));
		ps.executeUpdate();

		return true;
	}

	@Override
	public boolean changeRows(String tableName, String[] columns, String[] values) throws SQLException {
		if (tableName == null || 
			!this.doesTableExist(tableName)) return false;
		if (columns.length != values.length) throw new IllegalArgumentException();

		String updateQueryValues = "";
		for (int i = 0; i < columns.length; i++) {
			String column = columns[i];
			String value = values[i];
			updateQueryValues += String.format("%s = %s, ", column, value);
		}
		updateQueryValues = updateQueryValues.substring(0, updateQueryValues.length() - 2);
		
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("UPDATE %s SET %s;", tableName, updateQueryValues));
		ps.executeUpdate();
		
		return true;
	}
	
	@Override
	public boolean changeRows(String tableName, 
			String selector, String[] columns, String[] values) throws SQLException {
		if (tableName == null || 
			!this.doesTableExist(tableName)) return false;
		if (columns.length != values.length) throw new IllegalArgumentException();

		String updateQueryValues = "";
		for (int i = 0; i < columns.length; i++) {
			String column = columns[i];
			String value = values[i];
			updateQueryValues += String.format("%s = %s, ", column, value);
		}
		updateQueryValues = updateQueryValues.substring(0, updateQueryValues.length() - 2);
		
		PreparedStatement ps = this.connection.prepareStatement(
				String.format("UPDATE %s SET %s WHERE %s;", tableName, updateQueryValues, selector));
		ps.executeUpdate();
		
		return true;
	}
	
	@Override
	public boolean deleteRows(String tableName) throws SQLException {
		if (tableName == null || 
			!this.doesTableExist(tableName)) return false;

		PreparedStatement ps = this.connection.prepareStatement(
				String.format("DELETE FROM %s;", tableName));
		ps.executeUpdate();
		
		return true;
	}
	
	@Override
	public boolean deleteRows(String tableName, String selector) throws SQLException {
		if (tableName == null || 
			selector == null || 
			!this.doesTableExist(tableName)) return false;

		PreparedStatement ps = this.connection.prepareStatement(
				String.format("DELETE FROM %s WHERE %s;", tableName, selector));
		ps.executeUpdate();
		
		return true;
	}
}
