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

import com.google.common.collect.ImmutableMap;
import org.hupo.psi.calimocho.model.Row;
import org.junit.Test;
import org.string_db.EvidenceType;
import org.string_db.StringDbScores;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.string_db.psicquic.index.RowBuilderTest.assertRowsEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class StringdbRowBuilderTest {


    @Test
    public void build_rows_for_each_evidence_channel() throws Exception {
        final StringdbRowBuilder stringdbRowBuilder = new StringdbRowBuilder(new ConfidenceColumnBuilder(null) {
            @Override
            Map<EvidenceType, RowBuilder> buildRowForEachEvidenceType(StringDbScores scores) {
                return ImmutableMap.of(
                        EvidenceType.NEIGHBOURHOOD, new RowBuilder().withConfidence(771),
                        EvidenceType.COEXPRESSION, new RowBuilder().withConfidence(662)
                );
            }
        },
                new InteractorTypeFieldBuilder()
        );
        final List<Row> rows = stringdbRowBuilder.build(StringDbScores.builder(1, 2).build());

        assertEquals(2, rows.size());
        assertRowsEquals(new RowBuilder().withConfidence(771).withInteractorTypeA("MI:0326").withInteractorTypeB("MI:0326").build(), rows.get(0));
        assertRowsEquals(new RowBuilder().withConfidence(662).withInteractorTypeA("MI:0326").withInteractorTypeB("MI:0326").build(), rows.get(1));
    }

}
