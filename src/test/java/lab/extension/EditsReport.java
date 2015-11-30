/*
 * Copyright (C) 2015 Information Management Services, Inc.
 */
package lab.extension;

import java.util.ArrayList;
import java.util.List;

public class EditsReport {

    private List<EditFailure> _failures;

    public List<EditFailure> getFailures() {
        if (_failures == null)
            _failures = new ArrayList<>();
        return _failures;
    }
    
}
