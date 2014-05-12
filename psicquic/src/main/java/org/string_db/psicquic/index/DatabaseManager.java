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

import ch.uzh.molbio.common.db.DBException;
import ch.uzh.molbio.common.db.PostgresConnector;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A (buffering) iterator like interface over <code>PostgresConnector</code> for
 * loading scores data.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class DatabaseManager {

    private static final Logger log = Logger.getLogger(DatabaseManager.class);

    static final int FETCH_SIZE = 1000;

    private Long rowCounter = 0L;

    private final List<String> buffer = new ArrayList<String>();

    PostgresConnector db;

    ResultSet rs;

    MitabRecordBuilder rowBuilder;
    final DbUtil util = new DbUtil();
    private Integer spcId;
    private Map<String, String> setsCollections;
    private Map<Integer, String> uniprotIds;

    final String scoresQuery = "SELECT d.node_id_a, d.node_id_b, d.node_type_b, d.combined_score, d.evidence_scores"
            + " FROM  network.node_node_links d, items.species s "
            + "  WHERE s.species_id = d.node_type_b AND s.type ='core'  AND d.combined_score >= 400 "
            /*
                * this table is symmetrical but we only need one half since
                * psicuqic will expand queries to both ids:
                */
            + "    AND d.node_id_a < d.node_id_b AND d.node_type_b = ";

    DatabaseManager() {
        //for testing
    }

    DatabaseManager(PostgresConnector db, Integer spcId, Map<String, String> setsCollections, Map<Integer, String> uniprotIds) {
        this.db = db;
        this.spcId = spcId;
        this.setsCollections = setsCollections;
        this.uniprotIds = uniprotIds;
    }


    private synchronized void init() {
        if (rs != null) {
            return;
        }
        try {
            Map<Integer, List<String>> refseqIds = loadRefseqIds();
            rowBuilder = new MitabRecordBuilder(db, util.loadProteins(db, spcId), util.loadSpecies(db),
                    util.loadProteinsSets(db, spcId), setsCollections, uniprotIds, refseqIds);
            log.info("init scores cursor");
            this.rs = db.getCursorBasedResultSet(scoresQuery + spcId, FETCH_SIZE);
            log.info("done");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    boolean hasData() {
        fill(buffer);
        return !buffer.isEmpty();
    }


    String getNextRecord() {
        fill(buffer);
        if (buffer.isEmpty()) {
            //throw?
            return null;
        }
        return buffer.remove(0);
    }

    private void fill(List<String> bf) {
        if (!bf.isEmpty()) {
            return;
        }
        init();
        try {
            while (bf.isEmpty()) {
                if (rs.next() == false) {
                    log.info("no more data, records retrieved: " + rowCounter);
                    return;
                }
                if (++rowCounter % 10000 == 0) {
                    log.info(rowCounter + ". record read");
                }

                // unpack scores
                int score_neighb = 0, score_neighb_tr = 0, score_fusion = 0, score_cooccurrence = 0, score_coexpresion = 0,
                        score_coexpression_tr = 0, score_experimental = 0, score_experimental_tr = 0, score_database = 0,
                        score_database_tr = 0, score_textmining = 0, score_textmining_tr = 0;

                Integer[][] scores = (Integer[][]) rs.getArray(5).getArray();
                for (Integer[] score : scores) {
                    switch (score[0]) {
                        case 1:
                            score_neighb = score[1];
                            break;
                        case 2:
                            score_neighb_tr = score[1];
                            break;
                        case 3:
                            score_fusion = score[1];
                            break;
                        case 4:
                            score_cooccurrence = score[1];
                            break;
                        case 5:
                            /*ignore homology */
                            break;
                        case 6:
                            score_coexpresion = score[1];
                            break;
                        case 7:
                            score_coexpression_tr = score[1];
                            break;
                        case 8:
                            score_experimental = score[1];
                            break;
                        case 9:
                            score_experimental_tr = score[1];
                            break;
                        case 10:
                            score_database = score[1];

                            break;
                        case 11:
                            score_database_tr = score[1];
                            break;
                        case 12:
                            score_textmining = score[1];
                            break;
                        case 13:
                            score_textmining_tr = score[1];
                            break;
                        default:
                            break;
                    }
                }

                buffer.addAll(rowBuilder.makeMitabRecords(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        score_neighb, score_neighb_tr, score_fusion, score_cooccurrence, score_coexpresion,
                        score_coexpression_tr, score_experimental, score_experimental_tr, score_database,
                        score_database_tr, score_textmining, score_textmining_tr));

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<Integer, List<String>> loadRefseqIds() throws DBException, SQLException {
        Map<Integer, List<String>> r = new HashMap();
        ResultSet resultSet = db.execute("select protein_id, protein_name from items.proteins_names where species_id = " + spcId +
                "        and source = 'Ensembl_RefSeq';");
        while (resultSet.next()) {
            if (!r.containsKey(resultSet.getInt(1))) {
                r.put(resultSet.getInt(1), new ArrayList<String>());
            }
            r.get(resultSet.getInt(1)).add(resultSet.getString(2));
        }
        return r;
    }


    void shutdown() {
        log.info("shutting down");
        db.shutdown();
    }

}
