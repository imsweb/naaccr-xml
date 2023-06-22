/************************************************************************************************************;
    This programs demonstrates how to include and call the "read_naaccr_xml_macro.sas" macro.

    This example assumes that the JAR file is in the same folder as this program.

    Make sure that the naaccrVersion and recordType are correct or some data items won't be correctly populated.

    This example includes two items, meaning that only those two items will be included in the resulting
    date set. That parameter is optional and if not provided, the data set will contain all standard
    items plus any non-standard items provided via the extra dictionary. Be aware that creating a data set
    containing all items will be MUCH slower than creating one for just a few items, and so if you only need
    a handful of items to do your analysis, it is strongly recommended to provide those items (you can
    check the official NAACCR documentation to find the NAACCR XML IDs to use in that list).

    This example references an extra user-defined dictionary that defines non-standard NAACCR data items. If
    your data file only contains standard data items, that dictionary is not needed. Otherwise the dictionary
    should have been provided by the organization that created the XML data file. Dictionaries are usually
    in XML files, but for technical reasons, the macro expects them in CSV files; the NAACCR XML Tool that
    is distributed with the macros has an option to load a dictionary and save it as CSV.

 ************************************************************************************************************/;

%include "read_naaccr_xml_macro.sas";
%readNaaccrXml(
  libpath="naaccr-xml-9.2-sas.jar",
  sourcefile="synthetic-data_naaccr-210-incidence_10-tumors.xml",
  naaccrversion="210",
  recordtype="I",
  dataset=fromxml,
  items="patientIdNumber,primarySite"
);

proc freq data=fromxml;
    tables primarySite;
run;