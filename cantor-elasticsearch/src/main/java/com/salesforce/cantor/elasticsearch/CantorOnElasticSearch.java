package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.Events;
import com.salesforce.cantor.Maps;
import com.salesforce.cantor.Objects;
import com.salesforce.cantor.Sets;
import org.elasticsearch.client.Client;

public class CantorOnElasticSearch implements Cantor {

    private final Client client;

    public CantorOnElasticSearch(final Client client) {
        this.client = client;
    }

    @Override
    public Objects objects() {
        return null;
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
