%MACRO readNaaccrXml(libpath, sourcefile, naaccrversion="180", recordtype="I", dataset=alldata, items="", dictfile="");

/************************************************************************************************************;
    This macro reads a given NAACCR XML data file and loads the data into a dataset.

    Paramaters:
	- libpath needs to point to the Java SAS library (path can be relative or absolute)
	- sourcefile needs to point to the XML to import;
	    -- if the path ends with ".gz" it will be processed as a GZIP compressed file
	    -- if it ends with ".zip", every file inside the zip file will be processed (into the same SAS data set)
	    -- otherwise it will be processed as an uncompressed file (path can be relative or absolute)
	- naaccrversion should be "140", "150", "160" or "180" (defaults to "180")
	- recordtype should be "A", "M", "C" or "I" (defaults to "I")
    - dataset should be the name of the dataset into which the data should be loaded (defaults to alldata)
    - items is an optional CSV list of fields to read (any other fields will be ignored);
    - dictfile is the path to an optional user-defined dictionary in CSV format (see GUI tool to save an XML
        dictionary to CSV); use spaces to provide multiple paths

    Note that the macro creates a tmp CSV file in the same folder as the input file; that file will be 
    automatically deleted by the macro when it's done executing.

    Changelog
    *********
    06/10/2018 - Fabian Depry - Initial version.
    06/18/2018 - Fabian Depry - Added "replace" to CSV import proc.
    07/31/2018 - Fabian Depry - Added new optional parameter for user-defined dictionary.
    04/22/2020 - Fabian Depry - Improved comments, no change to the actual code.
 ************************************************************************************************************/;

/*
   Tell SAS where to find the Java library file.
*/
options set=CLASSPATH &libpath;

/*
   Call the Java library to create a tmp CSV file from the input XML file.
   The target CSV location can be provided as a parameter to the library but this code doesn't provide it; 
   in that case the library will use the same location and name (with a CSV extension) 
*/
data _null_;
    attrib csvpath length = $200;
    declare JavaObj j1 ('com/imsweb/naaccrxml/sas/SasXmlToCsv', &sourcefile, &naaccrversion, &recordtype);
    j1.callVoidMethod('setDictionary', &dictfile);
    j1.callStringMethod('getCsvPath', csvpath);
    call symput('csvfile', csvpath);
    j1.callVoidMethod('convert', &items);
    j1.delete();
run;

/*
   Import the tmp CSV file.
*/
proc import datafile="&csvfile" out=&dataset dbms=csv replace;
    getnames=yes;
run;

/*
   To force SAS to recognize all variables as text (and stop dropping leading 0's), the library adds a line of dashes;
   this code removes that fake line from the loaded dataset.
*/
data &dataset;
    set &dataset;
    if _n_ = 1 then delete;
run;

/*
    Cleanup the tmp CSV file.
*/
data _null_;    
    declare JavaObj j1 ('com/imsweb/naaccrxml/sas/SasXmlToCsv', &sourcefile, &naaccrversion, &recordtype);
    j1.callVoidMethod('cleanup');
    j1.delete();
run;

%MEND readNaaccrXml;