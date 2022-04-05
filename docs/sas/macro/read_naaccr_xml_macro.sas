%MACRO readNaaccrXml(libpath, sourcefile, naaccrversion="", recordtype="", dataset=alldata, items="", dictfile="", cleanupcsv="yes", groupeditems="no");

/************************************************************************************************************;
    This macro reads a given NAACCR XML data file and loads the data into a dataset.

    Paramaters:
	- libpath needs to point to the Java SAS library (path can be relative or absolute)
	- sourcefile needs to point to the XML to import (path can be relative or absolute);
	    -- if the path ends with ".gz" it will be processed as a GZIP compressed file
	    -- if it ends with ".zip", every file inside the zip file will be processed (into the same SAS data set);
	       the inner files can be compressed (".gz") or uncompressed.
	    -- otherwise it will be processed as an uncompressed file
	- naaccrversion should be one of the supported NAACCR versions provided as three digits:
	    "140", "150", "160", etc... (this parameter is required, no default);
	    make sure to provide the proper version or some items might be dropped during the reading process
	- recordtype should be "A", "M", "C" or "I" (required, no default);
	    make sure to provide the proper type or some items might be dropped during the reading process
    - dataset should be the name of the dataset into which the data should be loaded (defaults to alldata)
    - items is an optional list of items to read (any items not in the list will be ignored);
        if not provided, the data set will contain all standard items plus any non-standard items provided
        via the extra user-defined dictionary (if it was provided).
        There are two ways to provide the list:
            1. Hard code the XML IDs in the SAS code, separate them with a comma:
                   items="patientIdNumber,tumorRecordNumber,primarySite"
            2. Provide the path (relative or absolute) to a CSV file:
                   items="included-items.csv"
               The first line of the file must be headers; the XML IDs to include are expected to be found
               in the first column (the file can contain other columns); a simple file would look like this:
                   NAACCR_XML_ID
                   patientIdNumber
                   tumorRecordNumber
                   primarySite
        Be aware that creating a data set containing all items will be MUCH slower than creating one for
        just a few items, and so if you only need a handful of items to do your analysis, it is strongly
        recommended to provide those items.
        The NAACCR XML IDs for the standard items can be found on the NAACCR website.
    - dictfile is the path to an optional user-defined dictionary in CSV format (the NAACCR XML Tool that
        is distributed with the macros has an option to load an XML dictionary and save it as CSV);
        File*Pro can also generate those files); use spaces to separate multiple paths if you need to
        provide more than one dictionary
    - cleanupcsv should be "yes" or "no" (defaults to "yes"); if "no" then the tmp CSV file won't be
        automatically deleted; use this parameter to QC the CSV file or use it to investigate problems.
    - groupeditems should be "yes" or "no" (defaults to "no"); if "yes" then the grouped items will
        added to the created data set. Note that the "items" parameter has not impact on this one, either
        all the grouped items are included, or none are.

    Note that the macro creates a tmp CSV file in the same folder as the target file; that file will be
    automatically deleted by the macro when it's done executing (unless the 'cleanupcsv' parameter is set to 'no').

    Changelog
    *********
    06/10/2018 - Fabian Depry - Initial version.
    06/18/2018 - Fabian Depry - Added "replace" to CSV import proc.
    07/31/2018 - Fabian Depry - Added new optional parameter for user-defined dictionary.
    04/22/2020 - Fabian Depry - Improved comments, no change to the actual code.
    02/16/2021 - Fabian Depry - Fixed documentation missing version 210, no change to the actual code.
    03/12/2021 - Fabian Depry - Removed default value for version which was incorrectly set to 180.
    03/12/2021 - Fabian Depry - Removed default value for record type instead of assuming "I" for incidence.
    04/13/2021 - Fabian Depry - Added documentation for providing included items as a CSV file.
    10/08/2021 - Fabian Depry - Added new optional cleanupcsv parameter to allow better QC and problem investigation.
    04/04/2022 - Fabian Depry - Added new option groupeditems parameter to allow grouped items to be added to the data set.
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
    j1.callVoidMethod('setIncludeGroupedItems', &groupeditems);
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
    j1.callVoidMethod('cleanup', &cleanupcsv);
    j1.delete();
run;

%MEND readNaaccrXml;