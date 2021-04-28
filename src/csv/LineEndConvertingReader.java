package csv;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * Wrapper around a reader that converts all line endings (CR, LF and CR;LF) to LF (\n).
 */
public class LineEndConvertingReader implements AutoCloseable
{
	public static final char CR = '\r';
	public static final char LF = '\n';
	public static final int EOF = -1;
	public static final char EOL = LF;

	protected final Reader input;
	protected int nextChar;
	protected int line = 1;
	protected int column = 0;

	/**
	 * Wrap a reader to convert the line endings.
	 * 
	 * @param reader the underlying reader
	 * @throws IOException If an I/O error occurs
	 */
	public LineEndConvertingReader(Reader reader) throws IOException
	{
		this.input = Objects.requireNonNull(reader);
		this.nextChar = reader.read();
	}

	@Override
	public void close() throws IOException
	{
		input.close();
	}

	/**
	 * Get the current line number.
	 * 
	 * @return The current line number as an integer > 0
	 */
	int getLine()
	{
		return line;
	}

	/**
	 * Get the current column number.
	 * 
	 * @return The current column number as an integer > 0 or 0 if the reader is at the end of a line
	 */
	int getColumn()
	{
		return column;
	}

	/**
	 * Return whether the reader has another character other than {@code EOF}.
	 * 
	 * @return {@code true} if the next character is not {@code EOF}
	 */
	public boolean hasNext()
	{
		return nextChar != EOF;
	}

	/**
	 * Read a single character from the input
	 * 
	 * @return returns The next character as an integer in the range of 0 to 65536 or {@code EOF} (-1)
	 * @throws IOException If and I/O error occurs
	 */
	protected int read() throws IOException
	{
		if (nextChar == EOF) {
			// do not advance input beyond EOF
			return nextChar;
		}
		// advance
		int currentChar = nextChar;
		nextChar = input.read();
		if (currentChar == CR) {
			// replace CR and CR;LF by EOL
			currentChar = EOL;
			if (nextChar == LF) {
				nextChar = input.read();
			}
		}
		if (currentChar == EOL) {
			line += 1;
			column = 0;
		} else {
			column += 1;
		}
		return currentChar;
	}

	/**
	 * Peek the next character without advancing the underlying reader.
	 * 
	 * @return returns The next character as an integer in the range of 0 to 65536 or {@code EOF} (-1)
	 */
	public int peek()
	{
		// CR will be converted to EOL
		return (nextChar == CR) ? EOL : nextChar;
	}
}
