/**
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

import ch.uzh.molbio.common.db.PostgresConnector;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class DbUtil {
    private static final Logger log = Logger.getLogger(DbUtil.class);

    Map<Integer, Pair<String, String>> loadProteins(PostgresConnector db, Integer spcId) throws Exception {
        log.info("loading proteins");
        Map<Integer, Pair<String, String>> res = new HashMap<Integer, Pair<String, String>>();
        if (db == null) {
            return res;
        }
        ResultSet proteins = db.getCursorBasedResultSet(
                "select protein_id, protein_external_id, preferred_name from items.proteins "
                        + " where species_id = " + spcId, 10000);
        while (proteins.next()) {
            res.put(proteins.getInt(1), new Pair<String, String>(proteins.getString(2), proteins.getString(3)));
        }
        proteins.close();
        log.info(res.keySet().size() + " proteins read");
        return res;
    }

    Map<Integer, Pair<Integer, String>> loadSpecies(PostgresConnector db) {
        log.info("loading species");
        Map<Integer, Pair<Integer, String>> ids = new HashMap<Integer, Pair<Integer, String>>();
        if (db == null) {
            return ids;
        }
        try {
            ResultSet rs = db.execute("select species_id, official_name from items.species");
            while (rs.next()) {
                ids.put(rs.getInt(1), new Pair<Integer, String>(rs.getInt(1), rs.getString(2)));
            }
            rs.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info(ids.keySet().size() + " species read");
        return ids;
    }

    /**
     * evidence.sets_items table
     *
     * @param spcId
     */
    public Map<Integer, Set<String>> loadProteinsSets(PostgresConnector db, Integer spcId) {
        Map<Integer, Set<String>> map = new HashMap<Integer, Set<String>>();
        log.info("loading proteins sets");
        if (db == null) {
            return map;
        }
        try {
            ResultSet rs = db.execute("select protein_id, set_id from evidence.sets_items where protein_id > 0"
                    + " and species_id = " + spcId);
            while (rs.next()) {
                int protein = rs.getInt(1);
                if (!map.containsKey(protein)) {
                    map.put(protein, new HashSet<String>(2));
                }
                map.get(protein).add(rs.getString(2));
            }
            rs.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info(map.size() + " records read");
        return map;
    }

    /**
     * evidence.sets table
     */
    public Map<String, String> loadSetsCollections(PostgresConnector db) {
        Map<String, String> map = new HashMap<String, String>();
        log.info("loading sets collections");
        if (db == null) {
            return map;
        }
        try {
            ResultSet rs = db.execute("select set_id, collection_id from evidence.sets;");
            while (rs.next()) {
                map.put(rs.getString(1), rs.getString(2));
            }
            rs.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info(map.size() + " records read");
        return map;
    }
}
