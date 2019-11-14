package com.salesforce.cantor.elasticsearch;

import com.salesforce.cantor.Cantor;
import com.salesforce.cantor.common.AbstractBaseObjectsTest;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public class ObjectsOnElasticSearchTest extends AbstractBaseObjectsTest {

    @Override
    protected Cantor getCantor() throws IOException {
        return new CantorOnElasticSearch(new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200))));
    }
}