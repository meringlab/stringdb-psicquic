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
import org.junit.Ignore;
import org.junit.Test;
import org.string_db.DbFacade;
import org.string_db.ProteinExternalId;
import org.string_db.psicquic.AppProperties;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class DbFacadeITCase {
    static final DbFacade dbUtil = new DbFacade(
            AppProperties.instance.getProteinRepository(),
            AppProperties.instance.getSpeciesRepository(),
            AppProperties.instance.getGenericQueryProcessor());

    @Test
    public void test_speciesName() throws Exception {
        assertEquals("Homo sapiens", dbUtil.loadSpeciesName(9606));
    }

    @Test
    public void test_scoreTypes() throws Exception {
        final Map<Integer, String> types = dbUtil.loadScoreTypes();
        assertTrue(types.keySet().toString(), types.size() > 10);

    }

    @Test
    public void test_loadProteinExternalIds() throws Exception {
        final Map<Integer, ProteinExternalId> externalIds = dbUtil.loadProteinExternalIds(882);
        assertTrue("Desulfovibrio vulgaris  should have ~3000 proteins " + externalIds.size(), externalIds.size() > 3000);
        assertEquals("882.DVU0847", externalIds.get(4466).toString());
    }

    @Test
    public void test_proteinsSets() throws Exception {
        final Map<Integer, Set<String>> ids = dbUtil.loadProteinsSets(882);
        assertTrue("DVU0847 should have > 5 sets " + ids.get(4466).size(), ids.get(4466).size() > 5);
        assertTrue(ids.get(4466).toString(), ids.get(4466).contains("intact:EBI-11760304"));
        assertTrue("d.vulgaris should have >1000 proteins with sets" + ids.size(), ids.size() > 1000);
    }

    @Test
    public void test_refseqIds() throws Exception {
        final Map<Integer, Set<String>> ids = dbUtil.loadRefseqIds(882);
//        assertTrue(ids.get(4466).toString(), ids.get(4466).contains("aprA"));
        assertTrue(ids.get(4466).toString(), ids.get(4466).contains("DVU0847"));
        assertTrue("d.vulgaris should have >2000 proteins " + ids.size(), ids.size() > 2000);
    }

    @Ignore
    @Test
    public void test_sets() throws Exception {
        final Map<String, String> types = dbUtil.loadSetsCollections();
        assertTrue("this table should have millions of records: " + types.keySet().size(), types.size() > 3000000);
        assertEquals("dip", types.get("DIP-1123E"));
    }

    @Test
    public void test_speciesNames() throws Exception {
        Collection<String> names = dbUtil.loadSpeciesNames(9606);
        assertTrue(names.toString(), names.containsAll(ImmutableSet.of("Homo sapiens", "human")));
        names = dbUtil.loadSpeciesNames(10090);
        assertTrue(names.toString(), names.containsAll(ImmutableSet.of("Mus musculus", "mouse", "mice")));
        names = dbUtil.loadSpeciesNames(4932);
        assertTrue(names.toString(), names.containsAll(ImmutableSet.of("Saccharomyces cerevisiae", "yeast")));
    }

    @Test
    public void test_show_all_speciesNames() throws Exception {
        for (Integer speciesId : dbUtil.loadCoreSpecies()) {
            final Collection<String> names = dbUtil.loadSpeciesNames(speciesId);
            assertFalse(names.toString(), names.isEmpty());
//            if (names.size()  > 1)
//                System.out.println(speciesId + ": " + names);
        }
    }


}
