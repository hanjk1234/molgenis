package org.molgenis.data.elasticsearch.index;

import java.io.IOException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.molgenis.data.elasticsearch.ElasticSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchIndexCreator
{
	private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchService.class);
	
	public static final String DEFAULT_ANALYZER = "default";
	public static final String NGRAM_ANALYZER = "ngram_analyzer";
	private static final String NGRAM_TOKENIZER = "ngram_tokenizer";
	private static final String DEFAULT_TOKENIZER = "default_tokenizer";
	private static final String DEFAULT_STEMMER = "default_stemmer";
	
	private final Client client;

	public ElasticsearchIndexCreator(Client client)
	{
		this.client = client;
	}

	public void createIndexIfNotExists(String indexName) throws IOException
	{
		// Wait until elasticsearch is ready
		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
		boolean hasIndex = client.admin().indices().exists(new IndicesExistsRequest(indexName)).actionGet().isExists();
		if (!hasIndex)
		{
			createIndexInternal(indexName);
		}
	}

	public void createIndex(String indexName) throws IOException
	{
		// Wait until elasticsearch is ready
		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
		
		createIndexInternal(indexName);
	}
	
	private void createIndexInternal(String indexName) throws IOException {
		if (LOG.isTraceEnabled()) LOG.trace("Creating Elasticsearch index [" + indexName + "] ...");
		Builder settings = ImmutableSettings.settingsBuilder().loadFromSource(XContentFactory.jsonBuilder()
                .startObject()
                    .startObject("analysis")
                        .startObject("analyzer")
                            .startObject(DEFAULT_ANALYZER)
                                .field("type", "custom")
                                .field("filter", new String[]{"lowercase", DEFAULT_STEMMER})
                                .field("tokenizer", DEFAULT_TOKENIZER)
                                .field("char_filter", "html_strip")
                            .endObject()
                            .startObject(NGRAM_ANALYZER)
                                .field("type", "custom")
                                .field("filter", new String[]{"lowercase"})
                                .field("tokenizer", NGRAM_TOKENIZER)
                            .endObject()
                        .endObject()
	                    .startObject("filter")
	                    	.startObject(DEFAULT_STEMMER)
	                    		.field("type", "stemmer")
	                    		.field("name", "english")
	                    	.endObject()
	                    .endObject()
	                    .startObject("tokenizer")
	                    	.startObject(DEFAULT_TOKENIZER)
	                    		.field("type", "pattern")
	                    		.field("pattern", "([\\W\\.]+)")
	                    	.endObject()
	                    	.startObject(NGRAM_TOKENIZER)
	                    		.field("type", "nGram")
	                    		.field("min_gram", 1)
	                    		.field("max_gram", 10)
	                    	.endObject()
	                    .endObject()
                    .endObject()
                .endObject().string());
		CreateIndexResponse response = client.admin().indices().prepareCreate(indexName).setSettings(settings)
				.execute().actionGet();
		if (!response.isAcknowledged())
		{
			throw new ElasticsearchException("Creation of index [" + indexName + "] failed. Response=" + response);
		}
		LOG.info("Created Elasticsearch index [" + indexName + "]");
	}
}
