NAACCR XML
==========

This project is an attempt from the NAACCR XML Task Force to map the NAACCR flat-file format to an XML one.

### New to GitHub?

Since the project is public, you can view any files it contains using your browser.

The [/src/main/resources](https://github.com/depryf/naaccr-xml/tree/master/src/main/resources) folder contains the latest 
version of the dictionary used by the library.

Using the library
-----------------

There are three ways to use this library.

### Using streams
This is the recommended way to use the library; 4 streams are provided:
* [PatientXmlReader](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientXmlReader.java)
* [PatientXmlWriter](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientXmlWriter.java)
* [PatientFlatReader](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientFlatReader.java)
* [PatientFlatWriter](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/PatientFlatWriter.java)

The readers provide a ***readPatient()*** method that returns the next patient available, or null if the end of the stream is reached.
The writers provide a ***writePatient(patient)*** method.

Transforming a flat file into the corresponding XML file and vice-versa becomes very simple with those streams; just create the reader and writer  
you need and write every patient you read... 

### Using utility methods
There are few higher-level utility methods in the [NaaccrXmlUtils](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/NaaccrXmlUtils.java) class:

```java
    /**
     * Translates a flat data file into an XML data file.
     * @param flatFile source flat data file, must exists
     * @param xmlFile target XML data file
     * @param format expected NAACCR format
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @return the number of written patients
     * @throws IOException
     */
    public static int flatToXml(File flatFile, File xmlFile, String format, NaaccrDictionary nonStandardDictionary)
```

```java
    /**
     * Translates an XML data file into a flat data file.
     * @param xmlFile source XML data file, must exists
     * @param flatFile target flat data file
     * @param format expected NAACCR format
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @return the number of written records
     * @throws IOException
     */
    public static int xmlToFlat(File xmlFile, File flatFile, String format, NaaccrDictionary nonStandardDictionary)
```

```java
    /**
     * Reads an NAACCR XML data file and returns the corresponding data.
     * <br/>
     * ATTENTION: THIS METHOD WILL RETURN THE FULL CONTENT OF THE FILE AND IS NOT SUITABLE FOR LARGE FILE; CONSIDER USING A STREAM INSTEAD.
     * @param xmlFile source XML data file, must exists
     * @param format expected NAACCR format
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @throws IOException
     * @returns a <code>NaaccrDataExchange</code> object, never null
     */
    public static NaaccrDataExchange readXmlFile(File xmlFile, String format, NaaccrDictionary nonStandardDictionary)
```

```java
    /**
     * Writes the provided data to the requested XML file.
     * <br/>
     * ATTENTION: THIS METHOD REQUIRES THE ENTIRE DATA OBJECT TO BE IN MEMORY; CONSIDER USING A STREAM INSTEAD.
     * @param data a <code>NaaccrDataExchange</code> object, cannot be null
     * @param xmlFile target XML data file
     * @param format expected NAACCR format
     * @param nonStandardDictionary a user-defined dictionary for non-standard items (will be merged with the standard dictionary)
     * @throws IOException
     */
    public static void writeXmlFile(NaaccrDataExchange data, File xmlFile, String format, NaaccrDictionary nonStandardDictionary) throws IOException
```

There are other utility methods, but those are the main ones.

### Using the GUI

The library contains an experimental GUI that wraps some of the utility methods and provides a more user-friendly environment for translating files.

To start the GUI, just double-click the JAR file created from this project; it will invoke the main GUI class 
([Standlone](https://github.com/depryf/naaccr-xml/blob/master/src/main/java/org/naaccr/xml/gui/Standalone.java)).