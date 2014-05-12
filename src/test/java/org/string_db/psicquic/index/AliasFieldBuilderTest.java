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

import static org.string_db.psicquic.index.RowBuilderTest.assertRowsEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class AliasFieldBuilderTest {

    @Test
    public void both_columns() throws Exception {
        FieldBuilder aliasFieldBuilder = new AliasFieldBuilder(ImmutableMap.of(975673, "ARF5", 975854, "ARFIP2"));
        Row expected = new RowBuilder()
                .withAliasA("string", "ARF5")
                .withAliasB("string", "ARFIP2")
                .build();
        assertRowsEquals(expected, aliasFieldBuilder.proteins(975673, 975854).addTo(new RowBuilder()).build());
    }
}
