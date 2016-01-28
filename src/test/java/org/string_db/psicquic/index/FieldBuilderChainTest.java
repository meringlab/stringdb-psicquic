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
import com.google.common.collect.ImmutableSet;
import org.hupo.psi.calimocho.model.Row;
import org.hupo.psi.calimocho.tab.util.Mitab25ColumnKeys;
import org.hupo.psi.calimocho.tab.util.Mitab26ColumnKeys;
import org.junit.Test;
import org.string_db.ProteinExternalId;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class FieldBuilderChainTest {

    @Test
    public void test_chain() throws Exception {
        final RowBuilder recorder = new RowBuilder();
        final FieldBuilder chain = new IdsFieldBuilder(
                ImmutableMap.of(
                        975673, new ProteinExternalId("9606.ENSP00000000233"),
                        975854, new ProteinExternalId("9606.ENSP00000254584")
                ),
                ImmutableMap.of(975673, "P84085", 975854, "Q15027")
        );

        //put something that doesn't subclass TwoProteinsFieldAppender
        chain.chain(new InteractorTypeFieldBuilder());
        chain.chain(new RefseqAlternativeIdsFieldBuilder(
                ImmutableMap.of(975673, (Set<String>) ImmutableSet.of("NM_001662", "NP_001653.1"), 975854, ImmutableSet.of("XM_290852"))));

        chain.proteins(975673, 975854).addTo(recorder);

        final Row row = recorder.build();
        assertEquals(ImmutableSet.copyOf(Arrays.asList(
                Mitab25ColumnKeys.KEY_ID_A,
                Mitab25ColumnKeys.KEY_ID_B,
                Mitab25ColumnKeys.KEY_ALTID_A,
                Mitab25ColumnKeys.KEY_ALTID_B,
                Mitab26ColumnKeys.KEY_INTERACTOR_TYPE_A,
                Mitab26ColumnKeys.KEY_INTERACTOR_TYPE_B
        )), row.keySet());
    }
}
