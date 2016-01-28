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

import com.google.common.collect.ImmutableSet;
import org.string_db.EvidenceType;
import org.string_db.StringDbScores;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.string_db.EvidenceType.*;

/**
 * Creates a {@link org.hupo.psi.calimocho.model.Row} for each {@link org.string_db.EvidenceType}
 * except HOMOLOGY (and all transfer scores are combined into one).
 * <p/>
 * <p/>
 * <table>
 * <col width="25%"/>
 * <col width="75%"/>
 * <thead>
 * <tr><th>Channel</th><th>Ontology term</th></tr>
 * <thead>
 * <tbody>
 * <tr><td>textmining</td><td>"MI:0087”(predictive text mining)</td></tr>
 * <tr><td>co-occurrence</td><td>"MI:0085"(phylogenetic profile)</td></tr>
 * <tr><td>co-expressions</td><td>"MI:0686"(unspecified method - coexpression)</td></tr>
 * <tr><td>fusion</td><td>"MI:0036"(domain fusion)</td></tr>
 * <tr><td>neighbourhood</td><td>"MI:0057"(gene neighbourhood)</td></tr>
 * <tr><td>database</td><td>"MI:0362”(inferrence)</td></tr>
 * <tr><td>experimental</td><td>"MI:0045"(experimental interaction detection)</td></tr>
 * <tr><td>transfers</td><td>"MI:0064"(interologs mapping)</td></tr>
 * <p/>
 * </tbody>
 * </table>
 * <p/>
 * <p/>
 * Note: we cannot use 'inferred by curator' for database since, for example, we use pathways from KEGG
 * and infer scores (shouldn't this be treated as prediction?)
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href='https://docs.google.com/document/pub?id=11HpddNs-Bt5a4KOPGCXWivJ4MROYFbY2nhpk4PkvbTA'>Data Distribution Best Practices</a>
 */
class ConfidenceColumnBuilder {
    public static final Set<Pair<String, String>> STRINGDB_SOURCE = ImmutableSet.of(new Pair<String, String>("MI:1014", "string"));
    static final Map<EvidenceType, Pair<String, String>> detectionType = new HashMap();

    static {
        //should this be moved to a property file so it can be inspected and
        // modified without having to recompile the code?
        detectionType.put(TEXTMINING, new Pair("MI:0087", "predictive text mining"));
        detectionType.put(COOCCURRENCE, new Pair("MI:0085", "phylogenetic profile"));
        detectionType.put(COEXPRESSION, new Pair("MI:0686", "unspecified method - coexpression"));
        detectionType.put(FUSION, new Pair("MI:0036", "domain fusion"));
        detectionType.put(NEIGHBOURHOOD, new Pair("MI:0057", "gene neighbourhood"));
        detectionType.put(DATABASE, new Pair("MI:0362", "inference"));
        detectionType.put(EXPERIMENTAL, new Pair("MI:0045", "experimental interaction detection"));
        detectionType.put(COMBINED_TRANSFERRED, new Pair("MI:0064", "interologs mapping"));
    }

    private final SourceDbLookup sourceDbLookup;

    ConfidenceColumnBuilder(SourceDbLookup sourceDbLookup) {
        this.sourceDbLookup = sourceDbLookup;
    }

    Map<EvidenceType, RowBuilder> buildRowForEachEvidenceType(StringDbScores scores) {
        Map<EvidenceType, RowBuilder> result = new HashMap();
        for (EvidenceType evidenceType : detectionType.keySet()) {
            Integer score = getScore(scores, evidenceType);
            if (isZero(score) || score < StringDbScoresDataReader.MIN_SCORE) {
                continue;
            }
            final RowBuilder builder = new RowBuilder()
                    .withConfidence(score)
                    .withDetectionMethod(getTerm(evidenceType), getTermText(evidenceType))
                    .withInteractionType("psi-mi", EXPERIMENTAL.equals(evidenceType) ? "MI:0914" : "MI:1110");

            for (Pair<String, String> sourceDb : getSourceDbs(scores.getProteinA(), scores.getProteinB(), evidenceType)) {
                builder.withSourceDatabase(sourceDb.getX(), sourceDb.getY());
            }

            result.put(evidenceType, builder);
        }
        return result;
    }

    private Set<Pair<String, String>> getSourceDbs(Integer proteinA, Integer proteinB, EvidenceType evidenceType) {
        if (DATABASE.equals(evidenceType) || EXPERIMENTAL.equals(evidenceType)) {
            return sourceDbLookup.getSourceDbs(proteinA, proteinB);
        }
        return STRINGDB_SOURCE;
    }

    private boolean isZero(Integer score) {
        return score == null || score == 0;
    }

    private Integer getScore(StringDbScores scores, EvidenceType evidenceType) {
        return COMBINED_TRANSFERRED.equals(evidenceType) ? (scores.hasTransferred() ? scores.getTransferredScore() : null) : scores.get(evidenceType);
    }

    private String getTermText(EvidenceType evidenceType) {
        return detectionType.get(evidenceType).getY();
    }

    private String getTerm(EvidenceType evidenceType) {
        return detectionType.get(evidenceType).getX();
    }
}
