package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Objects;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.salesforce.cantor.common.ObjectsPreconditions.checkGet;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkKeys;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkNamespace;
import static com.salesforce.cantor.common.ObjectsPreconditions.checkStore;

public class ObjectsOnElasticSearch implements Objects {
    private static final Logger logger = LoggerFactory.getLogger(ObjectsOnElasticSearch.class);

    private static final String objectsIndexName = "cantor-objects-index";
    private static final String objectsTypeNameFormat = "cantor-objects-%s";

    private static final String fieldNameNamespace = "namespace";
    private static final String fieldNameKey = "key";
    private static final String fieldNameValue = "value";

    private static final Map<String, Object> objectsMappingSource = initializeMappingSource();

    private final Client client;

    public ObjectsOnElasticSearch(final Client client) throws IOException {
        this.client = client;
        try {
            this.client.admin().indices().prepareCreate(objectsIndexName).get();
        } catch (final Exception e) {
            logger.warn("exception caught creating objects index:", e);
            throw new IOException("exception creating objects index", e);
        }
    }

    @Override
    public Collection<String> namespaces() throws IOException {
        // todo: aggregation on field 'namespace'
        return null;
    }

    @Override
    public void create(final String namespace) throws IOException {
        checkNamespace(namespace);
        doCreate(namespace);
    }

    @Override
    public void drop(final String namespace) throws IOException {
        // todo: search and remove all keys matching 'namespace'
        // todo: also drop the type if we keep it
    }

    @Override
    public void store(final String namespace, final String key, final byte[] bytes) throws IOException {
        checkStore(namespace, key, bytes);
        doStore(namespace, key, bytes);
    }

    @Override
    public byte[] get(final String namespace, final String key) throws IOException {
        checkGet(namespace, key);
        return doGet(namespace, key);
    }

    @Override
    public boolean delete(final String namespace, final String key) throws IOException {
        return false;
    }

    @Override
    public Collection<String> keys(final String namespace, final int start, final int count) throws IOException {
        checkKeys(namespace, start, count);
        return doKeys(namespace, start, count);
    }

    @Override
    public int size(final String namespace) throws IOException {
        return 0;
    }

    private Collection<String> doKeys(final String namespace, final int start, final int count) {
        final SearchResponse response = this.client.prepareSearch(objectsIndexName)
                // bring back stored binary data
                .addStoredField(fieldNameValue)
                // create query for namespace/key
                .setQuery(getQuery(namespace, key))
                .get();
    }

    private byte[] doGet(final String namespace, final String key) {
        final SearchResponse response = this.client.prepareSearch(objectsIndexName)
                // bring back stored binary data
                .addStoredField(fieldNameValue)
                // create query for namespace/key
                .setQuery(getQuery(namespace, key))
                .get();
        final SearchHits hits = response.getHits();
        if (hits.getTotalHits() == null) {
            logger.info("got null hits back for object '{}.'{}'", namespace, key);
            return null;
        }

        logger.info("got {} total hits for '{}'.'{}'", hits.getTotalHits().value, namespace, key);
        final SearchHit hit = hits.getAt(0);
        final DocumentField field = hit.field(fieldNameValue);
        if (field != null && field.getValue() instanceof byte[]) {
            final byte[] bytes = field.getValue();
            logger.info("got {} bytes for '{}'.'{}'", bytes, namespace, key);
            return bytes;
        }

        logger.info("got hit back that doesn't have value field");
        return null;
    }

    private QueryBuilder getQuery(final String namespace, final String key) {
        return new BoolQueryBuilder()
                .must(new TermQueryBuilder(fieldNameNamespace, namespace))
                .must(new TermQueryBuilder(fieldNameKey, key));
    }


    private void doStore(final String namespace, final String key, final byte[] bytes) throws IOException {
        final IndexResponse response = this.client.prepareIndex().setIndex(objectsIndexName)
                // todo: remove this depending on removing types
                .setType(String.format(objectsTypeNameFormat, namespace))
                .setSource(XContentFactory.jsonBuilder()
                        .startObject()
                        .field(fieldNameNamespace, namespace)
                        .field(fieldNameKey, key)
                        .field(fieldNameValue, bytes)
                        .endObject()
                ).get();
        logger.info("got result '{}' indexing {} bytes for '{}'.'{}'", response.status(), bytes.length, namespace, key);
    }

    private void doCreate(final String namespace) throws IOException {
        // todo: we can potentially just get rid of this. Type is deprecated and pending removal
        final AcknowledgedResponse response = this.client.admin().indices().preparePutMapping(objectsIndexName)
                .setType(String.format(objectsTypeNameFormat, namespace))
                .setSource(objectsMappingSource)
                .get();
        if (!response.isAcknowledged()) {
            throw new IOException(String.format("failed to put mapping for namespace: '%s'", namespace));
        }
    }

    private static Map<String, Object> initializeMappingSource() {
        final Map<String, Object> source = new HashMap<>(1);
        final Map<String, Object> properties = new HashMap<>(3);
        // keyword fields 'namespace' and 'key' are how objects are queried
        properties.put(fieldNameNamespace, Collections.singletonMap("type", "keyword"));
        properties.put(fieldNameKey, Collections.singletonMap("type", "keyword"));
        // value field requires 'store' to be true to be stored/retrieved
        final Map<String, Object> valueProperties = new HashMap<>(2);
        valueProperties.put("type", "binary");
        valueProperties.put("store", true);
        properties.put(fieldNameValue, valueProperties);

        source.put("properties", properties);
        return source;
    }
}
