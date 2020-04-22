%MACRO writeNaaccrXml(libpath, targetfile, naaccrversion="180", recordtype="I", dataset=alldata, items="", dictfile="");

/************************************************************************************************************;
    This macro writes a given data fileset into a NAACCR XML data file.

    Paramaters:
	- libpath needs to point to the Java SAS library (path can be relative or absolute)
	- targetfile needs to point to the XML to export; if path ends with ".gz" it will be processed as a GZIP 
	  compressed file, otherwise it will be processed as an uncompressed file (path can be relative or absolute)
	- naaccrversion should be "140", "150", "160" or "180" (defaults to "180")
	- recordtype should be "A", "M", "C" or "I" (defaults to "I")
    - dataset should be the name of the dataset from which the data should be taken (defaults to alldata)
    - items is an optional CSV list of fields to write (any other fields will be ignored);
    - dictfile is the path to an optional user-defined dictionary in CSV format (see GUI tool to save an XML
        dictionary to CSV); use spaces to provide multiple paths
    - dictUri is an optional user-defined dictionary URI to reference in the created XML file (if a CSV dictionary
        is provided, then this one should be provided as well); use spaces to provide multiple URIs

    Note that the macro creates a tmp CSV file in the same folder as the target file; that file will be 
    automatically deleted by the macro when it's done executing.

    Changelog
    *********
    06/10/2018 - Fabian Depry - Initial version.
    07/31/2018 - Fabian Depry - Added new optional parameter for user-defined dictionary.
    12/08/2019 - Fabian Depry - Fixed a mistake in this comment, no change to the behavior of the macro.
    04/22/2020 - Fabian Depry - Added new dictUri parameter needed to properly re-create XML data files.
 ************************************************************************************************************/;

/*
   Tell SAS where to find the Java library file.
*/
options set=CLASSPATH &libpath;

/*
   Call the Java library to known the name and location of the CSV file it will expect.
*/
data _null_;
    attrib csvpath length = $200;
    declare JavaObj j1 ('com/imsweb/naaccrxml/sas/SasCsvToXml', &targetfile, &naaccrversion, &recordtype);
    j1.callStringMethod('getCsvPath', csvpath);
    call symput('csvfile', csvpath);
    j1.delete();
run;

/*
   Export the dataset into a CSV file.
*/
proc export data=&dataset
   outfile="&csvfile"
   dbms=csv
   replace;
run;

/*
   Call the Java library to convert the CSV file into an XML file; delete the CSV file once we are done.
*/
data _null_;
    declare JavaObj j1 ('com/imsweb/naaccrxml/sas/SasCsvToXml', &targetfile, &naaccrversion, &recordtype);
    j1.callVoidMethod('setDictionary', &dictfile &dictUri);
    j1.callVoidMethod('convert', &items);
    j1.callVoidMethod('cleanup');
    j1.delete();
run;


%MEND writeNaaccrXml;