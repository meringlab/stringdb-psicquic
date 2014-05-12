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

import org.junit.Test;
import org.string_db.UniprotAC;
import org.string_db.psicquic.AppProperties;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class UniprotIdsLoaderITCase {

    @Test
    public void test_load() throws Exception {
        UniprotIdsLoader loader = new UniprotIdsLoader();
        final Map<Integer, UniprotAC> ids = loader.loadUniprotIds(AppProperties.UNIPROT_IDS);
        assertEquals(new UniprotAC("P84085"), ids.get(975673));
    }
}
