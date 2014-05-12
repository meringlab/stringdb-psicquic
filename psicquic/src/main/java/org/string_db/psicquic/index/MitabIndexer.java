/**
 * Copyright 2014 The European Bioinformatics Institute, University of Zürich, SIB, and others.
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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.string_db.psicquic.AppProperties;
import psidev.psi.mi.search.Searcher;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Creates the PSICQUIC index based on the data from string-db: postgresql database + uniprot ids mapping list.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class MitabIndexer {

    private static final Logger log = Logger.getLogger(MitabIndexer.class);

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        PostgresConnector pg = openPostgresConnector();
        Map<Integer, String> uniprotIds = loadUniprotIds(AppProperties.UNIPROT_IDS);
        DbUtil util = new DbUtil();
        List<Integer> species = loadCoreSpecies(pg);
        Map<String, String> setsCollections = util.loadSetsCollections(pg);

        log.info("Creating index: " + AppProperties.properties.indexDir);
        File indexDirectory = openIndexDir(AppProperties.properties.indexDir);
        index(start, indexDirectory, pg, uniprotIds, species, setsCollections);
        log.info("indexing done in: " + ((System.currentTimeMillis() - start) / (1000 * 60)) + "min");
    }

    private static void index(long start, File indexDirectory, PostgresConnector pg, Map<Integer, String> uniprotIds, List<Integer> species, Map<String, String> setsCollections) {
        boolean create = true;
        for (Integer spcId : species) {
            try {
                log.info("indexing " + spcId + " (" + species.indexOf(spcId) + ". out of " + species.size() +
                        " in " + ((System.currentTimeMillis() - start) / (1000 * 60)) + "min)");
                long spc = System.currentTimeMillis();
                Searcher.buildIndex(indexDirectory, new StringPsimitabInputStream(new DatabaseManager(pg, spcId,
                        setsCollections, uniprotIds)), create, false);
                create = false;
                log.info(spcId + " done in: " + ((System.currentTimeMillis() - spc) / (1000 * 60)) + "min");
            } catch (Exception e) {
                log.error(e);
            }
            System.gc();
        }
    }

    static PostgresConnector openPostgresConnector() throws IOException, DBException {
        Properties props = new Properties();
        final FileInputStream inStream = new FileInputStream(AppProperties.HIBERNATE_PROPERTIES);
        props.load(inStream);
        inStream.close();

        String[] url = props.getProperty("hibernate.connection.url").replace("jdbc:postgresql://", "").split("/");
        final String host = url[0];
        final String database = url[1];

        final String username = props.getProperty("hibernate.connection.username");
        final String password = props.getProperty("hibernate.connection.password");
        log.info("host=" + host + ", db=" + database + ", user=" + username);

        return new PostgresConnector(host, database, username, password);
    }

    private static File openIndexDir(File indexDirectory) throws IOException {
        if (indexDirectory.exists()) {
            if (!indexDirectory.isDirectory()) {
                throw new IllegalArgumentException("index directory is a file!");
            }
            if (indexDirectory.exists()) {
                log.warn("index directory exists, deleting...");
                FileUtils.deleteDirectory(indexDirectory);
            }
        }
        return indexDirectory;
    }

    private static Map<Integer, String> loadUniprotIds(String file) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    String[] records = line.trim().split("\\s");
                    map.put(Integer.valueOf(records[0]), records[1]);
                } catch (Exception e) {
                    log.warn("bad record: " + line);
                }

            }
            r.close();
        } catch (Exception e) {//Catch exception if any
            throw new RuntimeException(e);
        }
        return map;
    }

    private static List<Integer> loadCoreSpecies(PostgresConnector pg) throws DBException, SQLException {
        List<Integer> species = new ArrayList<Integer>();
        ResultSet rs = pg.execute("select species_id from items.species where type = 'core'");
        while (rs.next()) {
            species.add(rs.getInt(1));
        }
        rs.close();
        return species;
    }
}
