/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab.extension;

import java.util.ArrayList;
import java.util.List;

public class EditsReport {

    private List<EditFailure> failures;

    public List<EditFailure> getFailures() {
        if (failures == null)
            failures = new ArrayList<>();
        return failures;
    }
    
}
