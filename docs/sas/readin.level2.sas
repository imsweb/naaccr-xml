filename Map "/prj/csb/testxml/sample/naaccr-xml-v16-sas-def-minimal.map";
filename testdata '/prj/csb/testxml/sample/naaccr-xml-v16-data-sample.xml';
libname testdata XMLV2 xmlmap=Map access=READONLY;

data patients; 
  set testdata.patients; 
 
proc sort; by patientkey; 

data tumors; 
  set testdata.tumors; 
  
proc sort; by patientkey; 

data seerxml; 
  merge patients tumors; 
    by patientkey; 
    
proc contents; 

proc print;     

proc freq; 
    
