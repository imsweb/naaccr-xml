filename xmldef 'naaccr-xml-sas-def-180-incidence-5-variables.map';
filename testdata pipe 'gunzip -c synthetic-data_naaccr-18-incidence_1000-recs.xml.gz';
libname testdata XMLV2 xmlmap=xmldef access=READONLY;

data patients; 
  set testdata.patients; 

data tumors; 
  set testdata.tumors; 

data seerxml; 
  merge patients tumors; 
    by patientkey; 
    
proc contents; 

proc freq; 
    tables primarySite;
run;
