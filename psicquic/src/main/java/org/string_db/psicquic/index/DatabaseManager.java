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
import java.sql.SQLException;
import java.util.ArrayList;
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

    final String scoresQuery = "SELECT d.protein_id_a, d.protein_id_b, d.species_id,  d.combined_score,"
            + "       d.equiv_nscore, d.equiv_nscore_transferred, d.equiv_fscore, d.equiv_pscore, d.equiv_hscore,"
            + "       d.array_score, d.array_score_transferred, d.experimental_score, d.experimental_score_transferred, "
            + "       d.database_score, d.database_score_transferred, d.textmining_score, d.textmining_score_transferred "
            + " FROM  network.protein_protein_links d, items.species s "
            + "  WHERE s.species_id = d.species_id AND s.type ='core'  AND d.combined_score >= 400 "
            /*
             * this table is symmetrical but we only need one half since
			 * psicuqic will expand queries to both ids:
			 */
            + "    AND d.protein_id_a < d.protein_id_b "
            + " and d.species_id = ";

    DatabaseManager() {
        //for testing
    }

    DatabaseManager(PostgresConnector db, Integer spcId, Map<String, String> setsCollections, Map<Integer, String> uniprotIds) {
        this.db = db;
        try {
            rowBuilder = new MitabRecordBuilder(db, util.loadProteins(db, spcId), util.loadSpecies(db),
                    util.loadProteinsSets(db, spcId), setsCollections, uniprotIds);
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
        try {
            while (bf.isEmpty()) {
                if (!rs.next()) {
                    log.info("no more data, records retrieved: " + rowCounter);
                    return;
                }
                if (++rowCounter % 10000 == 0) {
                    log.info(rowCounter + ". record read");
                }
                buffer.addAll(rowBuilder.makeMitabRecords(rs.getInt(1), rs.getInt(2), rs.getInt(3),

                        rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), /*
																		 * ignore
																		 * homology
																		 */rs.getInt(10), rs.getInt(11), rs.getInt(12),
                        rs.getInt(13), rs.getInt(14), rs.getInt(15), rs.getInt(16), rs.getInt(17)));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    void shutdown() {
        log.info("shutting down");
        db.shutdown();
    }

}
