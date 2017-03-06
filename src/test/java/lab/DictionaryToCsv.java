/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package lab;

import com.imsweb.naaccrxml.NaaccrFormat;
import com.imsweb.naaccrxml.NaaccrXmlDictionaryUtils;

public class DictionaryToCsv {

    public static void main(String[] args) {

        System.out.println("Item Number,Item Name,Item Start column,NAACCR XML ID,NAACCR XML Parent Element");
        NaaccrXmlDictionaryUtils.getMergedDictionaries(NaaccrFormat.NAACCR_VERSION_160).getItems().stream()
                .sorted((o1, o2) -> {
                    if (o1.getStartColumn() == null && o2.getStartColumn() == null)
                        return o1.getNaaccrId().compareTo(o2.getNaaccrId());
                    if (o1.getStartColumn() == null)
                        return 1;
                    if (o2.getStartColumn() == null)
                        return -1;
                    return o1.getStartColumn().compareTo(o2.getStartColumn());
                })
                .forEach(item -> System.out.println(item.getNaaccrNum() + ",\"" + item.getNaaccrName() + "\"," + item.getStartColumn() + "," + item.getNaaccrId() + "," + item.getParentXmlElement()));

    }

}
