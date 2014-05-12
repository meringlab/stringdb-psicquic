/**
 * Copyright 2014 The European Bioinformatics Institute, University of Zürich, SIB, and others.
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
package org.string_db.psicquic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import psidev.psi.mi.search.SearchResult;
import psidev.psi.mi.search.engine.SearchEngineException;
import psidev.psi.mi.search.engine.impl.BinaryInteractionSearchEngine;
import psidev.psi.mi.tab.model.BinaryInteraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unlike its super class, keeps IndexSearcher open all the time.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class StringBinaryInteractionSearchEngine extends BinaryInteractionSearchEngine {

    private static final Log log = LogFactory.getLog(StringBinaryInteractionSearchEngine.class);

    protected IndexSearcher indexSearcher;

    public StringBinaryInteractionSearchEngine(String indexDirectory) throws IOException {
        super(indexDirectory);
        try {
            indexSearcher = new IndexSearcher(indexDirectory);
        } catch (Exception e) {
            throw new SearchEngineException("Problem creating index searcher", e);
        }
    }

    @Override
    public SearchResult<BinaryInteraction> search(Query query, Integer firstResult, Integer maxResults, Sort sort) throws SearchEngineException {
        if (log.isDebugEnabled()) {
            log.debug("Searching=\"" + query + "\" (first=" + firstResult + "/max=" + maxResults + ")");
        }

        firstResult = firstResult == null ? 0 : firstResult;
        maxResults = maxResults == null ? Integer.MAX_VALUE : maxResults;

        long startTime = System.currentTimeMillis();
        try {
            Hits hits;

            if (sort != null) {
                hits = indexSearcher.search(query, sort);
            } else {
                hits = indexSearcher.search(query);
            }

            if (log.isDebugEnabled()) log.debug("\tTime: " + (System.currentTimeMillis() - startTime) + "ms");

            int totalCount = hits.length();

            if (totalCount < firstResult) {
                if (log.isDebugEnabled()) log.debug("\tNo hits. No results returned");
                return new SearchResult(Collections.EMPTY_LIST, totalCount, firstResult, maxResults, query);
            }

            int maxIndex = Math.min(totalCount, firstResult + maxResults);

            if (log.isDebugEnabled()) {
                log.debug("\tHits: " + hits.length() + ". Will return from " + firstResult + " to " + maxIndex);
            }
            List<BinaryInteraction> dataObjects = new ArrayList<BinaryInteraction>();

            for (int i = firstResult; i < maxIndex; i++) {
                Document doc = hits.doc(i);
                BinaryInteraction data = (BinaryInteraction) createDocumentBuilder().createData(doc);
                dataObjects.add(data);
            }

            return new SearchResult<BinaryInteraction>(dataObjects, totalCount, firstResult, maxResults, query);

        } catch (Exception e) {
            throw new SearchEngineException(e);
        }


    }

    @Override
    public SearchResult<BinaryInteraction> searchAll(Integer firstResult, Integer maxResults) throws SearchEngineException {

        if (firstResult == null) firstResult = 0;
        if (maxResults == null) maxResults = Integer.MAX_VALUE;

        IndexReader reader = indexSearcher.getIndexReader();

        int totalCount = reader.maxDoc();

        // this is a hack to ignore any header introduced in the index by mistake (first development versions)
        if (reader.isDeleted(0)) {
            firstResult++;
            totalCount--;
        }

        if (firstResult > totalCount) {
            return new SearchResult(Collections.EMPTY_LIST, totalCount, firstResult, maxResults, new WildcardQuery(new Term("", "*")));
        }

        int maxIndex = Math.min(totalCount, firstResult + maxResults);

        List<BinaryInteraction> dataObjects = new ArrayList<BinaryInteraction>();

        for (int i = firstResult; i < maxIndex; i++) {
            try {
                Document doc = reader.document(i);
                BinaryInteraction data = (BinaryInteraction) createDocumentBuilder().createData(doc);
                dataObjects.add(data);
            } catch (Exception e) {
                throw new SearchEngineException(e);
            }
        }

        return new SearchResult(dataObjects, totalCount, firstResult, maxResults, new WildcardQuery(new Term("", "*")));
    }
}
