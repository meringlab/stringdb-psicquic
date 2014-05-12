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
import org.junit.Test;
import org.string_db.EvidenceType;
import org.string_db.StringDbScores;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.string_db.psicquic.index.RowBuilderTest.assertRowsEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class ConfidenceColumnBuilderTest {

    @Test
    public void stringdb_predicted_scores() throws Exception {
        ConfidenceColumnBuilder confidenceExporter = new ConfidenceColumnBuilder(new SourceDbLookup(Collections.EMPTY_MAP, Collections.EMPTY_MAP) {
            @Override
            public Set<Pair<String, String>> getSourceDbs(Integer proteinA, Integer proteinB) {
                throw new RuntimeException("shouldn't be used for this evidence type");
            }
        });
        Map<EvidenceType, RowBuilder> rows = confidenceExporter.buildRowForEachEvidenceType(
                StringDbScores.builder(1, 2).with(EvidenceType.NEIGHBOURHOOD, 1).with(EvidenceType.FUSION, 12)
                        .with(EvidenceType.COOCCURRENCE, 44).with(EvidenceType.COEXPRESSION, 33).with(EvidenceType.TEXTMINING, 22).build()
        );
        assertEquals(5, rows.size());
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(1)
                        .withDetectionMethod("MI:0057", "gene neighbourhood")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:1014", "string")
                        .build(),
                rows.get(EvidenceType.NEIGHBOURHOOD).build()
        );
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(12)
                        .withDetectionMethod("MI:0036", "domain fusion")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:1014", "string")
                        .build(),
                rows.get(EvidenceType.FUSION).build()
        );
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(44)
                        .withDetectionMethod("MI:0085", "phylogenetic profile")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:1014", "string")
                        .build(),
                rows.get(EvidenceType.COOCCURRENCE).build()
        );
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(33)
                        .withDetectionMethod("MI:0686", "unspecified method - coexpression")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:1014", "string")
                        .build(),
                rows.get(EvidenceType.COEXPRESSION).build()
        );
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(22)
                        .withDetectionMethod("MI:0087", "predictive text mining")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:1014", "string")
                        .build(),
                rows.get(EvidenceType.TEXTMINING).build()
        );
    }

    @Test
    public void experimental_and_database_score() throws Exception {
        ConfidenceColumnBuilder confidenceExporter = new ConfidenceColumnBuilder(new SourceDbLookup(Collections.EMPTY_MAP, Collections.EMPTY_MAP) {
            @Override
            public Set<Pair<String, String>> getSourceDbs(Integer proteinA, Integer proteinB) {
                return ImmutableSet.of(new Pair<String, String>("MI:0463", "grid"));
            }
        });
        Map<EvidenceType, RowBuilder> rows = confidenceExporter.buildRowForEachEvidenceType(
                StringDbScores.builder(1, 2).with(EvidenceType.EXPERIMENTAL, 10).with(EvidenceType.DATABASE, 20).build()
        );
        assertEquals(2, rows.size());
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(10)
                        .withDetectionMethod("MI:0045", "experimental interaction detection")
                        .withInteractionType("psi-mi", "MI:0914")
                        .withSourceDatabase("MI:0463", "grid")
                        .build(),
                rows.get(EvidenceType.EXPERIMENTAL).build()
        );
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(20)
                        .withDetectionMethod("MI:0362", "inference")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:0463", "grid")
                        .build(),
                rows.get(EvidenceType.DATABASE).build()
        );

    }

    @Test
    public void transferred_score() throws Exception {
        ConfidenceColumnBuilder confidenceExporter = new ConfidenceColumnBuilder(new SourceDbLookup(Collections.EMPTY_MAP, Collections.EMPTY_MAP) {
            @Override
            public Set<Pair<String, String>> getSourceDbs(Integer proteinA, Integer proteinB) {
                throw new RuntimeException("shouldn't be used for this evidence type");
            }
        });
        Map<EvidenceType, RowBuilder> rows = confidenceExporter.buildRowForEachEvidenceType(
                StringDbScores.builder(1, 2)
                        .with(EvidenceType.NEIGHBOURHOOD_TRANSFERRED, 10)
                        .with(EvidenceType.COEXPRESSION_TRANSFERRED, 20)
                        .with(EvidenceType.EXPERIMENTAL_TRANSFERRED, 30)
                        .with(EvidenceType.TEXTMINING_TRANSFERRED, 50).build()
        );
        assertEquals("all transfer scores should be combined into one", 1, rows.size());
        assertRowsEquals(
                new RowBuilder()
                        .withConfidence(63)
                        .withDetectionMethod("MI:0064", "interologs mapping")
                        .withInteractionType("psi-mi", "MI:1110")
                        .withSourceDatabase("MI:1014", "string")
                        .build(),
                rows.get(EvidenceType.COMBINED_TRANSFERRED).build()
        );
    }
}
