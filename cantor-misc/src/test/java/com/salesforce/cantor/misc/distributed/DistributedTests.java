package com.salesforce.cantor.misc.distributed;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.h2.CantorOnH2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DistributedTests {
    private static final String path = "/tmp/cantor-distributed-test/" + UUID.randomUUID().toString();

    public static Cantor getCantor() throws IOException {
        return getCantor(ThreadLocalRandom.current().nextInt(3, 5), 1.0D);
    }

    public static Cantor getCantor(final int delegateCount, final double ratio) throws IOException {
        final List<Cantor> delegates = new ArrayList<>(delegateCount);
        for (int i = 0; i < delegateCount; i++) {
            delegates.add(new CantorOnH2(path + "/" + UUID.randomUUID().toString()));
        }
        return new DistributedCantor(delegates, ratio);
    }
}

