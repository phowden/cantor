package com.salesforce.cantor.misc.distributed;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.common.AbstractBaseObjectsTest;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs {@link DistributedObjects} through standard test suite, with successs ratio between 0.5 and 0.9 to determine
 * if tests can pass with less than total success required.
 */
public class DistributedObjectsPartialTest extends AbstractBaseObjectsTest {
    @Override
    protected Cantor getCantor() throws IOException {
        return DistributedTests.getCantor(10, ThreadLocalRandom.current().nextDouble(0.5D, 0.9D));
    }
}
