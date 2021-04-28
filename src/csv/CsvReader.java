package csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A class implementing a simple CSV reader according to RFC 4180.
 * To increase platform compatibility, we use LF (\n) as line separator and convert all other line endings accordingly.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>
 */
public class CsvReader implements AutoCloseable
{
	public static final int EOF = LineEndConvertingReader.EOF;
	public static final char EOL = LineEndConvertingReader.EOL;
	public static final char DOUBLE_QUOTES = '"';
	public static final char SEPARATOR = ',';
	public static final String[] STRING_ARRAY = new String[0];

	protected LineEndConvertingReader input;
	protected boolean hasNextRecord = true;
	protected final String[] header;
	protected int numFields;

	/**
	 * Create a CSV reader on an input.
	 * Assume the file starts with a header of distinct fields and all records have the same number of fields.
	 * 
	 * @param reader the underlying reader
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(Reader reader) throws IOException, CsvFormatException
	{
		this(reader, true, true);
	}

	/**
	 * Create a CSV reader on an input.
	 * If a header is present, assume it has distinct fields.
	 * 
	 * @param reader the underlying reader
	 * @param hasHeader treat the first line as header
	 * @param fixNumFields ensure all records have the same number of fields
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(Reader reader, boolean hasHeader, boolean fixNumFields) throws IOException, CsvFormatException
	{
		this(reader, hasHeader, fixNumFields, true);
	}

	/**
	 * Create a CSV reader on an input.
	 * 
	 * @param reader the underlying reader
	 * @param hasHeader treat the first line as header
	 * @param fixNumFields ensure all records have the same number of fields
	 * @param distinctFieldNames check that the header fields are distinct
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(Reader reader, boolean hasHeader, boolean fixNumFields, boolean distinctFieldNames) throws IOException, CsvFormatException
	{
		this.input = new LineEndConvertingReader(reader);
		this.numFields = fixNumFields ? 0 : -1;
		if (hasHeader) {
			header = nextRecord();
			if (! hasNextRecord()) {
				throw new CsvFormatException("no record found except for header", input.getLine());
			}
			if (distinctFieldNames) {
				Set<String>fieldNames = new HashSet<String>();
				Collections.addAll(fieldNames, header);
				if (fieldNames.size() != header.length) {
					throw new CsvFormatException("duplicated field names: " + Arrays.toString(header), 1);
				}
			}
		} else {
			header = null;
		}
	}

	@Override
	public void close() throws IOException
	{
		input.close();
	}

	/**
	 * Get the header if present.
	 * 
	 * @return The header of {@code null} if no header was expected
	 */
	public String[] getHeader()
	{
		return header;
	}

	/**
	 * Get the number of fields if fixed.
	 * 
	 * @return The number of fields or -1 if it is not fixed
	 */
	public int getNumberOfFields()
	{
		return numFields;
	}

	/**
	 * Get the current line number.
	 * 
	 * @return The current line number as an integer > 0
	 */
	public int getLine()
	{
		return input.getLine();
	}

	/**
	 * Return whether the CSV file has another record.
	 * 
	 * @return {@code true} if the file has another record
	 */
	public boolean hasNextRecord()
	{
		return hasNextRecord;
	}

	/**
	 * Get the next record.
	 * 
	 * @return The next record as an array of strings, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public String[] nextRecord() throws IOException, CsvFormatException
	{
		if (! hasNextRecord()) {
			throw new NoSuchElementException();
		}
		String[] nextRecord = readRecord();
		if (input.peek() == EOL) {
			input.read();
		}
		if (input.peek() == EOF) {
			hasNextRecord = false;
		}
		if (numFields > 0 && nextRecord.length != numFields) {
			throw new CsvFormatException("records contain different numbers of fields", input.getLine() - 1);
		} else if (numFields == 0) {
			numFields = nextRecord.length;
		}
		return nextRecord;
	}

	/**
	 * Read a record from the input.
	 * 
	 * @return The record as an array of strings, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String[] readRecord() throws IOException, CsvFormatException
	{
		List<String> record = new ArrayList<>();
		int next;
		do {
			String field = readField();
			record.add(field);
			next = input.peek();
			if (next == SEPARATOR) {
				input.read();
			}
		} while (! isEndOfRecord(next));
		return record.toArray(STRING_ARRAY);
	}

	/**
	 * Read a field from the input.
	 * 
	 * @return The field as string, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String readField() throws IOException, CsvFormatException
	{
		String quoted = readQuotedField();
		if (quoted == null) {
			return readPlainField();
		} else {
			return quoted;
		}
	}

	/**
	 * Read a quoted field from the input and strip the quotes.
	 * 
	 * @return The field as string, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String readQuotedField() throws IOException, CsvFormatException
	{
		if (input.peek() != DOUBLE_QUOTES) {
			return null;
		}
		input.read(); // Skip opening double quotes
		StringBuilder field = new StringBuilder();
		while (input.hasNext()) {
			int character = input.read();
			if (character == DOUBLE_QUOTES) {
				int next = input.peek();
				if (next == DOUBLE_QUOTES) {
					// 1. Escaped double quotes
					input.read();
				} else if (isEndOfField(next)) {
					// 2. Closing double quotes
					return field.toString();
				} else {
					// 3. Error
					throw new CsvFormatException("double quotes (\") in quoted field not escaped (\"\")", input.getLine(), input.getColumn());
				}
			}
			field.append((char) character);
		}
		throw new CsvFormatException("double quotes (\") missing to close quoted field", input.getLine(), input.getColumn());
	}

	/**
	 * Read a non-quoted field from the input.
	 * 
	 * @return The field as string, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String readPlainField() throws IOException, CsvFormatException
	{
		StringBuilder field = new StringBuilder();
		while (! isEndOfField(input.peek())) {
			int character = input.read();
			if (character == DOUBLE_QUOTES) {
				throw new CsvFormatException("double quotes (\") found in non-quoted field", input.getLine(), input.getColumn());
			}
			field.append((char) character);
		}
		return field.toString();
	}

	/**
	 * Check whether a character is the end of a field.
	 * 
	 * @param character the character to check
	 * @return {@code true} if the character is the end of a field
	 */
	protected static boolean isEndOfField(int character)
	{
		return character == SEPARATOR || isEndOfRecord(character);
	}

	/**
	 * Check whether a character is the end of a record.
	 * 
	 * @param character the character to check
	 * @return {@code true} if the character is the end of a record
	 */
	protected static boolean isEndOfRecord(int character)
	{
		return character == EOL || character == EOF;
	}

	/**
	 * Simple test to show how to use the CSV reader.
	 */
	public static void main(String[] args) throws IOException, CsvFormatException
	{
		String emptyCsv = "";
		System.out.println("CSV with single empty record:\n---");
		System.out.println(emptyCsv);
		System.out.println("---\nRecords:");
		CsvReader emptyRecords = new CsvReader(new BufferedReader(new StringReader(emptyCsv)), false, true);
		int i = 1;
		while (emptyRecords.hasNextRecord()) {
			System.out.println((i++) + ": " + Arrays.toString(emptyRecords.nextRecord()));
		}

		String mixedCsv = "h1,h2,h3\r"
				+ "plain,\"quoted\",\"quotes\"\"\",\"\"\n"
				+ ",1,2,3,4\n"
				+ "\r\n"
				+ ",,\n";
		System.out.println();
		System.out.println("CSV with mixed and quoted records:\n---");
		System.out.println(mixedCsv);
		System.out.println("---\nRecords:");
		CsvReader mixedRecords = new CsvReader(new BufferedReader(new StringReader(mixedCsv)), true, false);
		System.out.println("H: " + Arrays.toString(mixedRecords.getHeader()));
		int j = 1;
		while (mixedRecords.hasNextRecord()) {
			System.out.println((j++) + ": " + Arrays.toString(mixedRecords.nextRecord()));
		}
	}
}
