package com.salesforce.cantor.misc.distributed;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.common.AbstractBaseObjectsTest;

import java.io.IOException;

/**
 * Runs {@link DistributedObjects} through standard test suite, with successs ratio of 1.0
 */
public class DistributedObjectsTotalTest extends AbstractBaseObjectsTest {
    @Override
    protected Cantor getCantor() throws IOException {
        return DistributedTests.getCantor();
    }
}