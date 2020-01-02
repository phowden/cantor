package com.salesforce.cantor.misc.distributed;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.Events;
import com.salesforce.cantor.Maps;
import com.salesforce.cantor.Objects;
import com.salesforce.cantor.Sets;

import java.util.List;
import java.util.stream.Collectors;

public class DistributedCantor implements Cantor {

    private final DistributedObjects objects;

    public DistributedCantor(final List<Cantor> delegates, final double ratio) {
        this.objects = new DistributedObjects(delegates.stream().map(Cantor::objects).collect(Collectors.toList()), ratio);
    }

    @Override
    public Objects objects() {
        return this.objects;
    }

    @Override
    public Sets sets() {
        return null;
    }

    @Override
    public Maps maps() {
        return null;
    }

    @Override
    public Events events() {
        return null;
    }
}
