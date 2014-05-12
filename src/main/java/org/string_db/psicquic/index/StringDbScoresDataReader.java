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

import org.apache.log4j.Logger;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.string_db.DbFacade;
import org.string_db.StringDbScores;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * TODO test this class, against a sample or real database
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 * @see <a href='https://docs.google.com/document/pub?id=11HpddNs-Bt5a4KOPGCXWivJ4MROYFbY2nhpk4PkvbTA'>Data Distribution Best Practices</a>
 * XXX Write a DSL to serve as a documentation and possibility of changing the mapping without
 * having to recompile?
 */
class StringDbScoresDataReader implements DataReader<StringDbScores> {
    static final int FETCH_SIZE = 1000;
    private static final Logger log = Logger.getLogger(StringDbScoresDataReader.class);
    final PreparedStatement preparedStatement;
    /**
     * there's a lot of data to be indexed so let's filter out scores below 400.
     * Optionally, only 'core' species could be indexed.
     */
    private final String scoresQuery = "SELECT node_id_a, node_id_b, evidence_scores"
            + " FROM  network.node_node_links  "
            + "  WHERE combined_score >= 400  "
        /*
         * this table is symmetrical but we only need one half since
         * psicquic will expand queries to both ids:
         */
            + "    AND node_id_a < node_id_b "
            + "    AND node_type_b = ";
    private ResultSet rs;
    private Map<Integer, String> scoreTypes;
    /**
     * safety guard: prevents lost and duplicate rows.
     * A row can be lost if {@link #next()} is called twice without calling {@link #get()} in between.
     * A duplicate row can be return if {@link #get()} is called twice without calling {@link #next()} in between.
     */
    private boolean nextCalled;

    StringDbScoresDataReader(DbFacade dbFacade, JdbcTemplate jdbcTemplate, Integer speciesId) {
        log.info("init()");
        try {
            this.scoreTypes = dbFacade.loadScoreTypes();
            jdbcTemplate.setFetchSize(FETCH_SIZE);
            preparedStatement = jdbcTemplate.getDataSource().getConnection().prepareStatement(scoresQuery + speciesId);

//            this.rs = jdbcTemplate.queryForRowSet(scoresQuery + speciesId);
            this.rs = preparedStatement.executeQuery();
            nextCalled = false;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        log.info("done()");
    }

    @Override
    public boolean next() {
        if (nextCalled == true) {
            throw new IllegalStateException("next() called for the second time, call get() first");
        }
        nextCalled = true;
        try {
            final boolean next = rs.next();
            if (!next) {
                rs.close();
                preparedStatement.close();
            }
            return next;
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("failed to move the cursor", e);
        }

    }

    @Override
    public StringDbScores get() {
        if (nextCalled == false) {
            throw new IllegalStateException("call next() first!");
        }
        nextCalled = false;
        try {
            //SqlRowSet returns javax.sql.rowset.serial.SerialArray
            return new StringDbScores(rs.getInt(1), rs.getInt(2), scoreTypes, (Integer[][]) rs.getArray(3).getArray());
        } catch (SQLException e) {
            throw new DataRetrievalFailureException("failed to extract data", e);
        }
    }
}
