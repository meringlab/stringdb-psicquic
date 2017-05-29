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

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.string_db.DbFacade;
import org.string_db.ProteinRepository;
import org.string_db.UniprotAC;
import org.string_db.psicquic.AppProperties;

import java.io.FileWriter;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
@Ignore //this is outdated as of v10_5, must update stringdb-jdbc for the schema changes (items.proteins_names linkout->source)
public class UniprotIdsLoaderITCase {
    static JdbcTemplate db = AppProperties.instance.getJdbcTemplate();
    static ProteinRepository repo = AppProperties.instance.getProteinRepository();
    private static final DbFacade dbUtil = DbFacadeITCase.dbUtil;

    @Test
    public void test_load() throws Exception {
        final Map<Integer, UniprotAC> ids = repo.loadUniqueUniProtIds(511145);
        assertEquals(repo.count(511145).intValue(), ids.size());
        assertEquals(new UniprotAC("O32528"), ids.get(4737416));
    }

    @Test
    public void export_to_file() throws Exception {
        FileWriter writer = new FileWriter("string.uniprot.unique.ids.txt");
        for (Integer spcId : dbUtil.loadCoreSpecies()) {
            final Map<Integer, UniprotAC> ids = repo.loadUniqueUniProtIds(spcId);
            for (Integer id : ids.keySet()) {
                writer.write(id.toString());
                writer.write('\t');
                writer.write(ids.get(id).toString());
                writer.write('\n');
            }
            writer.flush();
        }
        writer.close();
    }

    @Ignore//this can take hours to finish..
    @Test
    public void test_if_uniprot_linkouts_are_unique() throws Exception {
        /*
        select distinct(linkout) from items.proteins_names where species_id = 9606
and linkout is not null
and protein_id in (
select distinct(protein_id) from items.proteins
where species_id = 9606 and protein_id not in (
select distinct(protein_id) from items.proteins_names where species_id = 9606
and linkout = 'UniProt'));

         */
        for (Integer spcId : dbUtil.loadCoreSpecies()) {
            final Integer totalNames = db.queryForObject("SELECT COUNT(*) FROM items.proteins_names " +
                    " WHERE source = 'UniProt' \n" +
                    " and species_id = " + spcId, Integer.class);
            final Integer uniqueNames = db.queryForObject("SELECT COUNT(DISTINCT(protein_id)) FROM items.proteins_names " +
                    " WHERE source = 'UniProt' \n" +
                    " and species_id = " + spcId, Integer.class);
            if (!totalNames.equals(uniqueNames)) {
                System.err.println(spcId + " doesn't have unique string_id <-> UniProt_id mapping: " + totalNames + " vs. " + uniqueNames);
//                Assert.fail("spcId + " doesn't have unique string_id <-> UniProt_id mapping: " + totalNames + " vs. " + uniqueNames");
            }
        }
    }
}
