# SimpleCsvJava

This project is a basic Java implementation of a CSV reader according to [RFC 4180](https://tools.ietf.org/html/rfc4180).
The line endings (CR, LF and CR;LF) and the field separator (comma by default) can be configured.
Facilities for line-end conversion are included.

## Example

```Java
// Import constants from CsvReader: CR, LF and COMMA
// Wrap a Reader, e.g. a BufferedReader on a file
BasicReader source = new BasicReader.Wrapper(reader);
// Normalize line endings
BasicReader normalized = source.convert(CR, LF).convert(CR).to(LF);
CsvReader csv = new CsvReader(normalized, COMMA, LF);
String[] header = csv.getHeader();
while (csv.hasNexRecord()) {
  String[] record = csv.nextRecord();
  // Do something with the record
} 
