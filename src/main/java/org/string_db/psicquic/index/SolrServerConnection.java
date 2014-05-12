/*
 * Copyright 2014 University of Zürich, SIB, and others.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.string_db.psicquic.index;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.hupo.psi.calimocho.model.Row;
import org.string_db.psicquic.SearchServer;
import psidev.psi.mi.calimocho.solr.converter.Converter;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class SolrServerConnection implements SearchServer {
    /**
     * max no. of indexing requests to buffer. Once the queue is full, it will block.
     */
    static final int REQUEST_QUEUE_SIZE = 1000;

    protected final SolrServer solrServer;
    protected final Converter solrDocsConverter = new Converter();

    SolrServerConnection(String solrUrl) {
        if (solrUrl == null) {
            throw new NullPointerException("No 'solr url' configured for SolrItemWriter");
        }
        this.solrServer = openConnection(solrUrl);
    }

    @Override
    public void add(Row row) throws RuntimeException {
        final UpdateResponse response;
        try {
            SolrInputDocument doc = solrDocsConverter.toSolrDocument(row);
            response = solrServer.add(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (0 != response.getStatus()) {
            throw new RuntimeException("operation failed: " + response);
        }
    }

    @Override
    public void commit(boolean reopenSearcher) throws RuntimeException {
        final UpdateResponse response;
        try {
            response = solrServer.commit(true, reopenSearcher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (0 != response.getStatus()) {
            throw new RuntimeException("operation failed: " + response);
        }
    }

    @Override
    public Long countIndexedDocuments() {
        try {
            final SolrDocumentList results = solrServer.query(new SolrQuery("*:*")).getResults();
            return results.getNumFound();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll() throws RuntimeException {
        final UpdateResponse response;
        try {
            response = solrServer.deleteByQuery("*:*");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (0 != response.getStatus()) {
            throw new RuntimeException("operation failed: " + response);
        }
    }

    private SolrServer openConnection(String solrUrl) {
        final SolrServer server;
        try {
//            server = new HttpSolrServer(solrUrl);
            // to enable authentication, i have to create and setup the HttpClient and give it to the ConcurrentUpdateSolrServer
            server = new ConcurrentUpdateSolrServer(solrUrl, REQUEST_QUEUE_SIZE,
                    2/*Runtime.getRuntime().availableProcessors()*/);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        final SolrPingResponse ping;
        try {
            ping = server.ping();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        if (ping.getStatus() != 0) {
            throw new ExceptionInInitializerError("solr not responding to ping: " + ping);
        }
        return server;
    }
}
