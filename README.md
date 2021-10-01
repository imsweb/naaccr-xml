# NAACCR XML

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.imsweb/naaccr-xml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.imsweb/naaccr-xml)

This library provides support for the NAACCR XML format. It implements version 1.5 of [the specifications](https://www.naaccr.org/xml-data-exchange-standard/) but still supports prior versions.

Information about the format and its specifications can be found in [the project wiki](https://github.com/imsweb/naaccr-xml/wiki).

## Download

The library is available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.imsweb%22%20AND%20a%3A%22naaccr-xml%22).

To include it to your Maven or Gradle project, use the group ID `com.imsweb` and the artifact ID `naaccr-xml`.

You can check out the [releases page](https://github.com/imsweb/naaccr-xml/releases) for a list of the releases and their changes.

This library requires Java 8 or a more recent version.

## Usage

There are four ways to use this library:

1. Using the stream classes
2. Using the NAACCR XML Utility class (NaaccrXmlUtils)
3. Using the Graphical User Interface (Standalone)
4. Using the no-GUI batch class (BatchProcessor)

### Using the stream classes
This is the recommended way to use the library; 4 streams are provided:
* [PatientXmlReader](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientXmlReader.java)
* [PatientXmlWriter](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientXmlWriter.java)
* [PatientFlatReader](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientFlatReader.java)
* [PatientFlatWriter](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/PatientFlatWriter.java)

The readers provide a ***readPatient()*** method that returns the next patient available, or null if the end of the stream is reached.
The writers provide a ***writePatient(patient)*** method.

Here is an example of reading a patient from a data file using the reader:

```java
try (PatientXmlReader reader = new PatientXmlReader(new FileReader(file), options, userDictionary)) {
    Patient patient = reader.readPatient();
}
```

Here is an example of writing a patient to a data file using the writer:

```java
 try (PatientXmlWriter writer = new PatientXmlWriter(new FileWriter(file), naaccrData, options, userDictionary)) {
    writer.writePatient(patient);
}
```

Note that the data files contain a collection of patients, but they also contain some information that appear once per file. 
That information is encapsulated in the NaaccrData class. The reading/writing process for that class is very different than the one used for the patients:

- The NaaccrData is read from XML when the constructor of the reader is called (so it's available as soon as the reader is created).
- The NaaccrData is written to XML when the constructor of the writer is called (so it's written to the file as soon as the writer is created).

It is important to keep in mind those side effects when creating readers and writers.

### Using the NAACCR XML Utility class (NaaccrXmlUtils)
A few higher-level utility methods have been defined in the [NaaccrXmlUtils](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrXmlUtils.java) class (only the required parameters are shown for clarity):

*Reading methods*
* NaaccrData ***readXmlFile*** (File xmlFile, ...)
* NaaccrData ***readFlatFile*** (File flatFile, ...)

*Writing methods*
* void ***writeXmlFile*** (NaaccrData data, File xmlFile, ...)
* void ***writeFlatFile*** (NaaccrData data, File flatFile, ...)

*Translation methods*
* void ***flatToXml*** (File flatFile, File xmlFile, ...)
* void ***xmlToFlat*** (File xmlFile, File flatFile, ...)
* Patient ***lineToPatient*** (String line, NaaccrContext context)
* String ***patientToLine*** (Patient patient, NaaccrContext context)

There are other utility methods, but those are the main ones.

All the file-related methods accept the following optional parameters (optional in the sense that null can be passed to the method):
* [NaaccrXmlOptions](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrXmlOptions.java) - options for customizing the read/write and errors reporting operations
* [NaaccrDictionary](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/entity/dictionary/NaaccrDictionary.java) - one or several user-defined dictionary (if none is provided, the default user-defined dictionary will be used)
* [NaaccrObserver](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/NaaccrObserver.java) - an observer allowing to report progress as the files are being processed.

The methods translating a single line or single patient takes a context as parameter; it is very important to initialize that context outside a loop if the methods are called in a loop.

### Using the Graphical User Interface (Standalone)

The library contains an standalone GUI that wraps some of the utility methods and provides a more user-friendly environment for processing files.

To start the GUI, unzip the distributed ZIP file anywhere on your computer and double-click the EXE file.

You can also type the following in a DOS prompt/terminal, after navigating to the folder containing the EXE file:
```
java -jar lib\naaccr-xml-utility.jar
```

### Using the no-GUI batch class (BatchProcessor)

The library also contains an experimental no-GUI class that can be used to process files in batch
([BatchProcessor](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/BatchProcessor.java)).

Here is an example of how to start it:
```
java -cp lib\naaccr-xml-utility.jar BatchProcessor options.properties
```

This assumes the options file is in the same folder as the EXE file (but it can be anywhere and a full path can be provided on the command line).

See the [BatchProcessor](https://github.com/imsweb/naaccr-xml/blob/master/src/main/java/com/imsweb/naaccrxml/BatchProcessor.java) class for a description of each individual option.

Here is how a typical options file would look like:

```properties
input.folder=C:\\input
processing.mode=flat-to-xml
output.folder=C:\\output
```

## About this library

This library was developed through the [SEER](http://seer.cancer.gov/) program.

The Surveillance, Epidemiology and End Results program is a premier source for cancer statistics in the United States.
The SEER program collects information on incidence, prevalence and survival from specific geographic areas representing
a large portion of the US population and reports on all these data plus cancer mortality data for the entire country.