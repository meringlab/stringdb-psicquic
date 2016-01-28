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
import org.string_db.ProteinExternalId;

import static org.string_db.psicquic.index.RowBuilderTest.assertRowsEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class IdsFieldApenderTest {

    final FieldBuilder idsFieldBuilder = new IdsFieldBuilder(
            ImmutableMap.of(
                    975673, new ProteinExternalId("9606.ENSP00000000233"),
                    975854, new ProteinExternalId("9606.ENSP00000254584")
            ),
            ImmutableMap.of(975673, "P84085", 975854, "Q15027")
    );
    final Row expected = new RowBuilder()
            .withIdA(new ProteinExternalId("9606.ENSP00000000233")).withIdA("P84085")
            .withIdB(new ProteinExternalId("9606.ENSP00000254584")).withIdB("Q15027")
            .build();
    final RowBuilder recorder = new RowBuilder();

    @Test
    public void both_columns() throws Exception {
        idsFieldBuilder.proteins(975673, 975854).addTo(recorder);
        assertRowsEquals(expected, recorder.build());
    }

    @Test
    public void append_called_only_once() throws Exception {
        idsFieldBuilder.proteins(975673, 975854);
        idsFieldBuilder.addTo(recorder);
        idsFieldBuilder.addTo(recorder);
        assertRowsEquals(expected, recorder.build());
    }
}
