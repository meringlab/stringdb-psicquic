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

import org.hupo.psi.calimocho.model.Row;
import org.string_db.DbFacade;
import org.string_db.StringDbScores;
import org.string_db.UniprotAC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Takes STRINGDB's data (proteins, mappings, evidence, sequences...)
 * and produces {@link org.hupo.psi.calimocho.model.Row}s.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class StringdbRowBuilder {

    protected final ConfidenceColumnBuilder scoresParser;
    protected final FieldBuilder appenderChain;


    StringdbRowBuilder(ConfidenceColumnBuilder scoresParser, FieldBuilder appenderChain) {
        this.scoresParser = scoresParser;
        this.appenderChain = appenderChain;
    }

    static Builder builder(DbFacade util) {
        return new Builder(util);
    }

    public List<Row> build(StringDbScores scores) {
        List<Row> results = new ArrayList<Row>();
        for (RowBuilder rowBuilder : scoresParser.buildRowForEachEvidenceType(scores).values()) {
            appenderChain.proteins(scores.getProteinA(), scores.getProteinB()).addTo(rowBuilder);
            results.add(rowBuilder.build());
        }
        return results;
    }

    static class Builder {
        private DbFacade util;

        Builder(DbFacade util) {
            this.util = util;
        }

        /**
         * Return a newly created instance with all the
         * {@link FieldBuilder} chained.
         *
         * @param speciesId
         * @param uniprotIds
         * @return
         */
        StringdbRowBuilder build(Integer speciesId, Map<Integer, UniprotAC> uniprotIds) {
            try {
                Map<String, String> setsCollections = util.loadSetsCollections();
                Map<Integer, Set<String>> proteinsSets = util.loadProteinsSets(speciesId);
                final ConfidenceColumnBuilder scoresBuilder = new ConfidenceColumnBuilder(new SourceDbLookup(proteinsSets, setsCollections));

                FieldBuilder appenderChain = new IdsFieldBuilder(util.loadProteinExternalIds(speciesId), uniprotIds);
                appenderChain.chain(new RefseqAlternativeIdsFieldBuilder(util.loadRefseqIds(speciesId)));
                appenderChain.chain(new AliasFieldBuilder(util.loadProteinNames(speciesId)));
//                appenderChain.chain(new TaxonFieldBuilder(speciesId, util.loadSpeciesName(speciesId)));
                appenderChain.chain(new MultipleTaxonNamesFieldBuilder(speciesId, util.loadSpeciesNames(speciesId)));


                appenderChain.chain(new InteractorTypeFieldBuilder());
                appenderChain.chain(new RogidFieldBuilder(speciesId, util.loadProteinSequences(speciesId)));
                return new StringdbRowBuilder(scoresBuilder, appenderChain);
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

}
