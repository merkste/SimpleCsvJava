package csv;

/**
 * Class to signal syntax errors in CSV files. 
 */
public class CsvFormatException extends Exception
{
	private static final long serialVersionUID = -445612337957123597L;

	private final int line;
	private final int column;

	/**
	 * Create an exception with a line number.
	 * 
	 * @param message the description of the error
	 * @param line the line number of the error
	 */
	public CsvFormatException(String message, int line)
	{
		this(message, line, 0);
	}

	/**
	 * Create an exception with a line number and a column number.
	 * 
	 * @param message the description of the error
	 * @param line the line number of the error
	 * @param column the column number of the error
	 */
	public CsvFormatException(String message, int line, int column)
	{
		super(message + inputPosition(line, column));
		this.line = line;
		this.column = column;
	}

	/**
	 * Get the line number of the error.
	 * 
	 * @return The line number of the error or {@code null}
	 */
	public int getLine()
	{
		return line;
	}

	/**
	 * Get the line number of the error.
	 * 
	 * @return The column number of the error or {@code null}
	 */
	public int getColumn()
	{
		return column;
	}

	/**
	 * Print a string describing the position of the error.
	 * 
	 * @param line the line number of the error
	 * @param column the column number of the error
	 * @return The position the error if {@code line > 0}.
	 */
	protected static String inputPosition(int line, int column)
	{
		String position = "";
		if (line > 0) {
			position += " (input line: " + line;
			if (column > 0) {
				position += ", column: " + column + ")";
			}
			position += ")";
		}
		return position;
	}
}
