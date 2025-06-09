/*
 * Copyright (C) 2025 Information Management Services, Inc.
 */
package lab;

import java.io.IOException;
import java.util.List;

import com.imsweb.naaccr.api.client.NaaccrApiClient;
import com.imsweb.naaccr.api.client.entity.NaaccrDataItem;

public class EvaluateNewVersion {

    public static void main(String[] args) throws IOException {
        List<NaaccrDataItem> items1 = NaaccrApiClient.getInstance().getDataItems("25");
        System.out.println("Got " + items1.size() + " items");
        List<NaaccrDataItem> items2 = NaaccrApiClient.getInstance().getDataItems("26");
        System.out.println("Got " + items2.size() + " items");
    }

}
