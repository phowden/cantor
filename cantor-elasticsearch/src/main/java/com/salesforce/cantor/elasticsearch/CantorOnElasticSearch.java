package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.Events;
import com.salesforce.cantor.Maps;
import com.salesforce.cantor.Objects;
import com.salesforce.cantor.Sets;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CantorOnElasticSearch implements Cantor {
    private static final Logger logger = LoggerFactory.getLogger(CantorOnElasticSearch.class);
    private final Objects objects;
    private final Sets sets;
    private final Maps maps;
    private final Events events;

    public CantorOnElasticSearch(final RestHighLevelClient client) throws IOException {
        this.objects = new ObjectsOnElasticSearch(client);
        this.sets = null;
        this.maps = null;
        this.events = null;
    }

    @Override
    public Objects objects() {
        return this.objects;
    }

    @Override
    public Sets sets() {
        return this.sets;
    }

    @Override
    public Maps maps() {
        return this.maps;
    }

    @Override
    public Events events() {
        return this.events;
    }
}
