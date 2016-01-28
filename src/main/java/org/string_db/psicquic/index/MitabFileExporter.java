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
import org.hupo.psi.calimocho.io.IllegalRowException;
import org.hupo.psi.calimocho.model.Row;
import org.hupo.psi.calimocho.tab.io.DefaultRowWriter;
import org.hupo.psi.calimocho.tab.util.MitabDocumentDefinitionFactory;
import org.string_db.DbFacade;
import org.string_db.StringDbScores;
import org.string_db.psicquic.AppProperties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class MitabFileExporter {
    protected static final AppProperties appProperties = AppProperties.instance;
    static final DbFacade db = new DbFacade(
            appProperties.getProteinRepository(),
            appProperties.getSpeciesRepository(),
            appProperties.getGenericQueryProcessor());
    static final Logger log = Logger.getLogger(MitabFileExporter.class);
    protected static String OUT_DIR = "output/";
    protected final Map<Integer, String> uniprotIds;

    public MitabFileExporter() throws Exception {
        final Map<Integer, Set<String>> linkouts = db.loadUniProtLinkouts();
        this.uniprotIds = readUniProtIds(linkouts);
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        log.info("indexing to: " + OUT_DIR);
        final List<Integer> speciesIds = db.loadSpeciesIds();
        final MitabFileExporter exporter = new MitabFileExporter();

        for (Integer spcId : speciesIds) {
            System.gc();
            final String outputFile = OUT_DIR + spcId + "-mitab." + AppProperties.STRINGDB_VERSION + "_" + AppProperties.BUILD_NUMBER + ".txt";
            if (new File(outputFile).exists()) {
                log.info("skipping, output file exists " + outputFile);
                continue;
            }
            log.info("exporting " + spcId + " (" + (1 + speciesIds.indexOf(spcId)) + ". out of " + speciesIds.size() +
                    " in " + ((System.currentTimeMillis() - start) / (1000 * 60)) + "min)");
            exporter.exportSpecies(spcId, outputFile);
        }

        log.info("export done in: " + ((System.currentTimeMillis() - start) / (1000 * 60)) + "min");
    }

    private static void writeToFile(List<Row> rows, String outputFile) {
        log.debug("writing " + rows.size() + " lines to " + outputFile);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(outputFile);
        } catch (IOException e) {
            throw new RuntimeException("failed to open " + outputFile, e);
        }
        DefaultRowWriter writer = new DefaultRowWriter(MitabDocumentDefinitionFactory.mitab25());
        try {
            writer.write(fileWriter, rows);
        } catch (IOException e) {
            log.error("failed to write rows", e);
        } catch (IllegalRowException e) {
            log.error("invalid row", e);
        }
    }

    private Map<Integer, String> readUniProtIds(Map<Integer, Set<String>> linkouts) {
        Map<Integer, String> map = new HashMap<>();
        for (Map.Entry<Integer, Set<String>> e : linkouts.entrySet()) {
            if (e.getValue().isEmpty()) {
                log.warn("UniProt id missing for " + e.getKey());
                continue;
            }
            if (e.getValue().size() > 1) {
                log.warn("multiple UniProt ids for " + e.getKey() + ": " + e.getValue());
            }

            final String linkout = e.getValue().iterator().next();
            String uniprotId = linkout.substring(linkout.lastIndexOf("/") + 1);
            map.put(e.getKey(), uniprotId);
        }
        return map;
    }

    void exportSpecies(Integer spcId, String outputFile) throws Exception {
        final long spc = System.currentTimeMillis();

        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(outputFile);
        } catch (IOException e) {
            throw new RuntimeException("failed to open " + outputFile, e);
        }
        DefaultRowWriter writer = new DefaultRowWriter(MitabDocumentDefinitionFactory.mitab25());

        final StringdbRowBuilder stringdbRowBuilder = StringdbRowBuilder.builder(db).build(spcId, this.uniprotIds);
        final StringDbScoresDataReader scoresReader = new StringDbScoresDataReader(db, AppProperties.instance.getJdbcTemplate(), spcId);
        log.debug("scores reader created, exporting...");
        long numInteractions = 0;
        while (scoresReader.next()) {
            final StringDbScores stringDbScores = scoresReader.get();
            for (final Row row : stringdbRowBuilder.build(stringDbScores)) {
                final String mitabRecord = writer.writeLine(row);
                try {
                    fileWriter.append(mitabRecord);
                    numInteractions++;
                } catch (IOException e) {
                    log.error("failed to write row: " + mitabRecord, e);
                }
            }
        }
        fileWriter.close();
        log.info(spcId + " total interactions: " + numInteractions + ", done in: " + ((System.currentTimeMillis() - spc) / (1000)) + "sec");
    }
}
