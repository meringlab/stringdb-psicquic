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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class SourceDbLookupTest {

    final Map<Integer, Set<String>> ps = new HashMap();
    final Set<String> p1set = new HashSet();
    final Set<String> p2set = new HashSet();
    final Map<String, String> sc = new HashMap();

    SourceDbLookup cut = new SourceDbLookup(ps, sc);

    @Before
    public void setUp() throws Exception {
        p1set.add("PDB_2b6h");
        p1set.add("biogrid:193555");
        p1set.add("hprd:35158");
        p1set.add("BCID:11510");
        ps.put(975673, p1set);

        p2set.add("biogrid:154256");
        p2set.add("biogrid:193555");//common
        p2set.add("irefindex:AqUwGAnKlayVm2H+Q2PHMjbQX4A(rigid)");
        p2set.add("BCID:11510");//common

        ps.put(975854, p2set);
        sc.put("biogrid:172154", "grid");
        sc.put("hprd:35158", "hrpd");
        sc.put("biogrid:154256", "grid");
        sc.put("biogrid:193555", "grid");
        sc.put("BCID:11510", "bind");
    }


    @Test
    public void testGetSourceDbs() throws Exception {
        final Set<Pair<String, String>> sourceDbs = cut.getSourceDbs(975673, 975854);
        Assert.assertEquals(2, sourceDbs.size());
        assertTrue(sourceDbs.toString(), sourceDbs.contains(new Pair("MI:0463", "biogrid")));
        assertTrue(sourceDbs.toString(), sourceDbs.contains(new Pair("MI:0462", "bind")));
    }
}
