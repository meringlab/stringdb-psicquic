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

import org.apache.cxf.feature.Features;
import org.apache.lucene.search.BooleanQuery;
import org.hupo.psi.mi.psicquic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.search.SearchResult;
import psidev.psi.mi.search.engine.SearchEngine;
import psidev.psi.mi.search.engine.impl.BinaryInteractionSearchEngine;
import psidev.psi.mi.tab.converter.tab2xml.Tab2Xml;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.builder.MitabDocumentDefinition;
import psidev.psi.mi.xml.converter.impl254.EntrySetConverter;
import psidev.psi.mi.xml.dao.inMemory.InMemoryDAOFactory;
import psidev.psi.mi.xml254.jaxb.Attribute;
import psidev.psi.mi.xml254.jaxb.AttributeList;
import psidev.psi.mi.xml254.jaxb.Entry;
import psidev.psi.mi.xml254.jaxb.EntrySet;

import java.io.IOException;
import java.util.*;

/**
 * PSIMITAB Lucene based PSICQUIC web service. Derived from <a href="http://code.google.com/p/psicquic/source/browse/trunk/psicquic-webservice/src/main/java/org/hupo/psi/mi/psicquic/ws/IndexBasedPsicquicService.java"
 * >IndexBasedPsicquicService.java</a>
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
@Controller
@Features(features = {"org.apache.cxf.transport.http.gzip.GZIPFeature"})
public class StringdbPsicquicService implements PsicquicService {

    private static final Logger logger = LoggerFactory.getLogger(StringdbPsicquicService.class);

    public static final String RETURN_TYPE_XML25 = "psi-mi/xml25";
    public static final String RETURN_TYPE_MITAB25 = "psi-mi/tab25";
    public static final String RETURN_TYPE_MITAB25_BIN = "psi-mi/tab25-bin";
    public static final String RETURN_TYPE_COUNT = "count";

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final int BLOCKSIZE_MAX = 200;
    private static final String RETURN_TYPE_DEFAULT = RETURN_TYPE_MITAB25;

    public static final List<String> SUPPORTED_RETURN_TYPES = Arrays.asList(RETURN_TYPE_XML25, RETURN_TYPE_MITAB25,
            RETURN_TYPE_COUNT);

    @SuppressWarnings("unchecked")
    private SearchEngine<BinaryInteraction> searchEngine;

    Map<String, String> properties = new HashMap<String, String>();

    public StringdbPsicquicService() {
        // XXX potential performance bottleneck
        BooleanQuery.setMaxClauseCount(200 * 1000);

        properties = new HashMap<String, String>();
        properties.put("psicquic.spec.version", "1.1");
        properties.put("psicquic.implementation.name", "string-db PSICQUIC Implementation");
        properties.put("psicquic.implementation.version", getVersion());

        try {
            searchEngine = new BinaryInteractionSearchEngine(AppProperties.properties.indexDir);
        } catch (IOException e) {
            logger.error("", e);
            throw new RuntimeException("Error creating SearchEngine using directory: " + AppProperties.properties.indexDir, e);
        }
    }

    public QueryResponse getByInteractor(DbRef dbRef, RequestInfo requestInfo) throws NotSupportedMethodException,
            NotSupportedTypeException, PsicquicServiceException {
        String query = createQuery("identifiers", dbRef);

        return getByQuery(query, requestInfo);
    }

    public QueryResponse getByInteraction(DbRef dbRef, RequestInfo requestInfo) throws NotSupportedMethodException,
            NotSupportedTypeException, PsicquicServiceException {
        String query = createQuery("interaction_id", dbRef);

        return getByQuery(query, requestInfo);
    }

    public QueryResponse getByInteractorList(List<DbRef> dbRefs, RequestInfo requestInfo, String operand)
            throws NotSupportedMethodException, NotSupportedTypeException, PsicquicServiceException {
        String query = createQuery("identifiers", dbRefs, operand);

        return getByQuery(query, requestInfo);
    }

    public QueryResponse getByInteractionList(List<DbRef> dbRefs, RequestInfo requestInfo)
            throws PsicquicServiceException, NotSupportedMethodException, NotSupportedTypeException {
        String query = createQuery("interaction_id", dbRefs, "OR");

        return getByQuery(query, requestInfo);
    }

    private String createQuery(String fieldName, DbRef dbRef) {
        return createQuery(fieldName, Collections.singleton(dbRef), null);
    }

    private String createQuery(String fieldName, Collection<DbRef> dbRefs, String operand) {
        StringBuilder sb = new StringBuilder(dbRefs.size() * 64);
        sb.append(fieldName).append(":(");

        for (Iterator<DbRef> dbRefIterator = dbRefs.iterator(); dbRefIterator.hasNext(); ) {
            DbRef dbRef = dbRefIterator.next();

            sb.append(createQuery(dbRef));

            if (dbRefIterator.hasNext()) {
                sb.append(" ").append(operand).append(" ");
            }
        }

        sb.append(")");

        return sb.toString();
    }

    private String createQuery(DbRef dbRef) {
        String db = dbRef.getDbAc();
        String id = dbRef.getId();

        return "(" + ((db == null || db.length() == 0) ? "\"" + id + "\"" : "\"" + db + "\" AND \"" + id + "\"") + ")";
    }

    public QueryResponse getByQuery(String query, RequestInfo requestInfo) throws NotSupportedMethodException,
            NotSupportedTypeException, PsicquicServiceException {
        final int blockSize = Math.min(requestInfo.getBlockSize(), BLOCKSIZE_MAX);

        final String resultType = requestInfo.getResultType();

        if (resultType != null && !getSupportedReturnTypes().contains(resultType)) {
            throw new NotSupportedTypeException("Not supported return type: " + resultType + " - Supported types are: "
                    + getSupportedReturnTypes());
        }

        // if (!new File(config.getIndexDirectory()).exists()) {
        // throw new
        // PsicquicServiceException("Lucene directory does not exist: "+config.getIndexDirectory());
        // }

        logger.debug("Searching: {} ({}/{})", new Object[]{query, requestInfo.getFirstResult(), blockSize});

        SearchResult searchResult = searchEngine.search(query, requestInfo.getFirstResult(), blockSize);

        // preparing the response
        QueryResponse queryResponse = new QueryResponse();
        ResultInfo resultInfo = new ResultInfo();
        resultInfo.setBlockSize(blockSize);
        resultInfo.setFirstResult(requestInfo.getFirstResult());
        resultInfo.setTotalResults(searchResult.getTotalCount());

        queryResponse.setResultInfo(resultInfo);

        ResultSet resultSet = createResultSet(query, searchResult, requestInfo);
        queryResponse.setResultSet(resultSet);

        return queryResponse;
    }

    public String getVersion() {
        return "1.0";
    }

    public List<String> getSupportedReturnTypes() {
        return SUPPORTED_RETURN_TYPES;
    }

    public List<String> getSupportedDbAcs() {
        return Collections.EMPTY_LIST;
    }

    public String getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public List<Property> getProperties() {
        List<Property> props = new ArrayList<Property>();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Property prop = new Property();
            prop.setKey(entry.getKey());
            prop.setValue(entry.getValue());
            props.add(prop);
        }

        return props;
    }

    protected ResultSet createResultSet(String query, SearchResult searchResult, RequestInfo requestInfo)
            throws PsicquicServiceException, NotSupportedTypeException {
        ResultSet resultSet = new ResultSet();

        String resultType = (requestInfo.getResultType() != null) ? requestInfo.getResultType() : RETURN_TYPE_DEFAULT;

        if (RETURN_TYPE_MITAB25.equals(resultType)) {
            if (logger.isDebugEnabled())
                logger.debug("Creating PSI-MI TAB");

            String mitab = createMitabResults(searchResult);
            resultSet.setMitab(mitab);
        } else if (RETURN_TYPE_XML25.equals(resultType)) {
            if (logger.isDebugEnabled())
                logger.debug("Creating PSI-MI XML");

            EntrySet jEntrySet = createEntrySet(searchResult);
            resultSet.setEntrySet(jEntrySet);

            // add some annotations
            if (!jEntrySet.getEntries().isEmpty()) {
                AttributeList attrList = new AttributeList();

                Entry entry = jEntrySet.getEntries().iterator().next();

                Attribute attr = new Attribute();
                attr.setValue("Data retrieved using the PSICQUIC service. Query: " + query);
                attrList.getAttributes().add(attr);

                Attribute attr2 = new Attribute();
                attr2.setValue("Total results found: " + searchResult.getTotalCount());
                attrList.getAttributes().add(attr2);

                // add warning if the batch size requested is higher than the
                // maximum allowed
                if (requestInfo.getBlockSize() > BLOCKSIZE_MAX && BLOCKSIZE_MAX < searchResult.getTotalCount()) {
                    Attribute attrWarning = new Attribute();
                    attrWarning.setValue("Warning: The requested block size (" + requestInfo.getBlockSize()
                            + ") was higher than the maximum allowed (" + BLOCKSIZE_MAX + ") by PSICQUIC the service. "
                            + BLOCKSIZE_MAX + " results were returned from a total found of "
                            + searchResult.getTotalCount());
                    attrList.getAttributes().add(attrWarning);

                }

                entry.setAttributeList(attrList);
            }

        } else if (RETURN_TYPE_COUNT.equals(resultType)) {
            if (logger.isDebugEnabled())
                logger.debug("Count query");
            // nothing to be done here
        } else {
            throw new NotSupportedTypeException("Not supported return type: " + resultType + " - Supported types are: "
                    + getSupportedReturnTypes());
        }

        return resultSet;
    }

    protected String createMitabResults(SearchResult searchResult) {
        MitabDocumentDefinition docDef = new MitabDocumentDefinition();

        List<BinaryInteraction> binaryInteractions = searchResult.getData();

        StringBuilder sb = new StringBuilder(binaryInteractions.size() * 512);

        for (BinaryInteraction binaryInteraction : binaryInteractions) {
            String binaryInteractionString = docDef.interactionToString(binaryInteraction);
            sb.append(binaryInteractionString);
            sb.append(NEW_LINE);
        }
        return sb.toString();
    }

    private EntrySet createEntrySet(SearchResult searchResult) throws PsicquicServiceException {
        if (searchResult.getData().isEmpty()) {
            return new EntrySet();
        }

        Tab2Xml tab2Xml = new Tab2Xml();
        try {
            psidev.psi.mi.xml.model.EntrySet mEntrySet = tab2Xml.convert(searchResult.getData());

            EntrySetConverter converter = new EntrySetConverter();
            converter.setDAOFactory(new InMemoryDAOFactory());

            return converter.toJaxb(mEntrySet);

        } catch (Exception e) {
            throw new PsicquicServiceException("Problem converting results to PSI-MI XML", e);
        }
    }
}
