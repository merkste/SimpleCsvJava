# SimpleCsvJava

This project is a basic Java implementation of a CSV reader according to [RFC 4180](https://tools.ietf.org/html/rfc4180).
The line endings (CR, LF and CR;LF) and the field separator (comma by default) can be configured.
Facilities for line-end conversion are included.

## Example

```Java
// Import constants
import static BasicReader.LF;
import static CsvReader.COMMA;

// Wrap a Reader, e.g. a BufferedReader on a file
BasicReader source = BasicReader.wrap(reader);

// Normalize line endings if required
BasicReader normalized = source.normalizeLineEndings();

// Read CSV with COMMA as field separator and LF as line separator
CsvReader csv = new CsvReader(normalized, COMMA, LF);

// Access the header if enabled and present
String[] header = csv.getHeader();

// Iterate over all records with checked Exceptions
while (csv.hasNexRecord()) {
  String[] record = csv.nextRecord();
  // Do something with the record
}

// Iterate over all records with unchecked Exceptions
for (String[] record : csv) {
  // Do something with the record
}
