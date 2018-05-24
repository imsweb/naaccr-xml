proc template;

    /***********************************************************************************/
    /*                                  NAACCR XML Format.                             */
	/*                                                                                 */
	/* !!! This templates assumes that all variables are available in the data set !!! */
    /***********************************************************************************/

  define tagset tagsets.naaccrxml;  
  
    define event doc;
    start:
        put "<?xml version=""1.0""?>" nl nl;
        put "<NaaccrData baseDictionaryUri=""http://naaccr.org/naaccrxml/naaccr-dictionary-";
		put $options['naaccr_version'];
		put ".xml"" recordType=""";
		put $options['record_type'];
		put """ specificationVersion=""1.3"" xmlns=""http://naaccr.org/naaccrxml"">" nl;
        eval $data_written 0;
        eval $pat_written 0;
        eval $in_data 0;
        eval $in_pat 0;
        eval $in_tum 0;
        eval $cur_pat 0;
        set $prev_pat '0';
        ndent;
        break;
    finish:
        trigger endPatient /if $in_tum = 1;
        xdent;
        put "</NaaccrData>";
        break;
    end;
    
  define event row;
    start:
        eval $in_data 0;
        eval $in_pat 0;
        eval $in_tum 0;
        break;
    finish:
        trigger endTumor / if $in_tum = 1;
        break;
  end;

  define event data;
      start:
      do / if cmp(NAME, "naaccrDataKey");
          eval $in_data 1;
          set $cur_data VALUE;
      done;
      break / if cmp(NAME, "naaccrDataKey");

      do / if cmp(NAME, "patientKey");
          eval $in_data 0;
          eval $in_pat 1;
          set $cur_pat VALUE;
          do / if !cmp($cur_pat,$prev_pat);
              trigger endPatient / if $data_written = 1;
              trigger startPatient;
              eval $pat_written 0;
          done;
          set $prev_pat VALUE;
          eval $data_written 1;
      done;
      break / if cmp(NAME, "patientKey");

      do / if cmp(NAME, "tumorKey");
          eval $in_data 0;
          eval $in_pat 0;
          eval $in_tum 1;
      eval $pat_written 1;
          trigger startTumor;
      done;
      break / if cmp(NAME, "tumorKey");

      break / if $in_data = 1 and $data_written = 1;
      break / if $in_pat = 1 and $pat_written = 1;
      
      break / if cmp(VALUE, "");
  
      put "<Item naaccrId=""";
      put NAME;
      put """";
      
      put ">";
      put VALUE;
      put "</Item>" nl;
      break;
    end;
  
  define event startPatient;
    put "<Patient>" nl;
    ndent;
    break;
  end;

  define event endPatient;
    xdent;
    put "</Patient>" nl;
    break;
  end;

  define event startTumor;
    put "<Tumor>" nl;
    ndent;
    break;
  end;

  define event endTumor;
    xdent;
    put "</Tumor>" nl;
    break;
  end;
  
  indent = 4;
end;  
run;