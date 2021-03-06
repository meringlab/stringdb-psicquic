/**
 * Copyright 2014 University of Zürich, SIB, and others.
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.string_db;

import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.string_db.jdbc.GenericQueryProcessor;
import org.string_db.jdbc.TwoColumnRowMapper;

import java.sql.SQLException;
import java.util.*;

/**
 * Helper class to data from PostgreSQL
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class DbFacade {
    private static final Logger log = Logger.getLogger(DbFacade.class);
    protected final ProteinRepository proteinRepository;
    protected final SpeciesRepository speciesRepository;
    protected final GenericQueryProcessor queryProcessor;
    protected final Map<String, String> scoreTypeMap = new HashMap<>();

    public DbFacade(ProteinRepository proteinRepository, SpeciesRepository speciesRepository, GenericQueryProcessor queryProcessor) {
        this.proteinRepository = proteinRepository;
        this.speciesRepository = speciesRepository;
        this.queryProcessor = queryProcessor;

        scoreTypeMap.put("equiv_nscore", "neighbourhood");
        scoreTypeMap.put("equiv_nscore_transferred", "neighbourhood_transferred");
        scoreTypeMap.put("equiv_fscore", "fusion");
        scoreTypeMap.put("equiv_pscore", "cooccurrence");
        scoreTypeMap.put("equiv_hscore", "homology");
        scoreTypeMap.put("array_score", "coexpression");
        scoreTypeMap.put("array_score_transferred", "coexpression_transferred");
        scoreTypeMap.put("experimental_score", "experimental");
        scoreTypeMap.put("experimental_score_transferred", "experimental_transferred");
        scoreTypeMap.put("database_score", "database");
        scoreTypeMap.put("database_score_transferred", "database_transferred");
        scoreTypeMap.put("textmining_score", "textmining");
        scoreTypeMap.put("textmining_score_transferred", "textmining_transferred");
        scoreTypeMap.put("neighborhood_score", "neighborhood");
        scoreTypeMap.put("fusion_score", "fusion");
        scoreTypeMap.put("cooccurence_score", "cooccurence");

    }

    public List<Integer> loadCoreSpecies() {
        return speciesRepository.loadCoreSpeciesIds();
    }

    public List<Integer> loadSpeciesIds() {
        return speciesRepository.loadSpeciesIds();
    }

    /**
     * evidence.sets_items table
     *
     * @param spcId
     */
    public Map<Integer, Set<String>> loadProteinsSets(Integer spcId) {
        log.info("loading proteins sets");
        Map<Integer, Set<String>> map =
                queryProcessor.selectTwoColumns("item_id", "set_id", "evidence.sets_items",
                        TwoColumnRowMapper.<Integer, String>multiValMapper(),
                        "item_id > 0 and species_id = :species_id; ",
                        new MapSqlParameterSource("species_id", spcId));

        log.info(map.size() + " proteins.sets records read");
        return map;
    }


    public Map<Integer, String> loadScoreTypes() throws SQLException {
        final Map<Integer, String> crypticTypes = queryProcessor.selectTwoColumns("score_id", "score_type", "network.score_types",
                TwoColumnRowMapper.<Integer, String>uniqueValMapper());
        Map<Integer, String> types = new LinkedHashMap<>();
        for (Integer id : crypticTypes.keySet()) {
            final String crypticType = crypticTypes.get(id);
            if (!this.scoreTypeMap.containsKey(crypticType))  {
                throw new RuntimeException("missing score type: " + crypticType);
            }
            types.put(id, this.scoreTypeMap.get(crypticType));
        }
        return types;
    }

    /**
     * evidence.sets table
     */
    public Map<String, String> loadSetsCollections() {
        log.info("loading sets collections");
        final Map<String, String> map = queryProcessor.selectTwoColumns("set_id", "collection_id", "evidence.sets",
                TwoColumnRowMapper.<String, String>uniqueValMapper());
        log.info(map.size() + " records read");
        return map;
    }

    /**
     * <pre>select distinct(source) from items.proteins_names
     * where  LOWER(source) like '%refseq%'
     * --returns
     * "RefSeq"
     * "Ensembl_RefSeq"
     * "Ensembl_HGNC_RefSeq_IDs"
     * "Ensembl_RefSeq_synonym"
     * "Ensembl_RefSeq_short"
     * </pre>
     * <p/>
     * We'll skip "Ensembl_RefSeq_synonym" and "Ensembl_RefSeq_short".
     *
     * @param spcId
     * @return
     */
    public Map<Integer, Set<String>> loadRefseqIds(Integer spcId) throws SQLException {
        log.info("loadRefseqIds");
        final Map<Integer, Set<String>> names = proteinRepository.loadProteinNames(spcId,
                ImmutableSet.of("Ensembl_RefSeq", "Ensembl_HGNC_RefSeq_IDs", "RefSeq"));
        log.info(names.size() + " names read");
        return names;
    }

    public Map<Integer, ProteinExternalId> loadProteinExternalIds(Integer spcId) throws SQLException {
        log.info("loadProteinExternalIds");
        final Map<Integer, ProteinExternalId> ids = proteinRepository.loadExternalIds(spcId);
        log.info(ids.size() + " ids read");
        return ids;
    }

    public Map<Integer, String> loadProteinNames(Integer spcId) throws SQLException {
        log.info("loadProteinNames");
        final Map<Integer, String> names = proteinRepository.loadProteinPreferredNames(spcId);
        log.info(names.size() + " names read");
        return names;
    }

    public Map<Integer, String> loadProteinSequences(Integer spcId) {
        log.info("loadProteinSequences");
        final Map<Integer, String> sequences = proteinRepository.loadProteinSequences(spcId);
        log.info(sequences.size() + " sequences read");
        return sequences;
    }

    public String loadSpeciesName(Integer speciesId) {
        return speciesRepository.loadSpeciesName(speciesId);
    }

    /**
     * Seems like items.species_names has lots of not really useful names,
     * so let's just pick short, one-word ones, that start with lower case,
     * e.g. human for 9606, mouse&mice for 10090, yeast for 4932, etc.
     *
     * @param speciesId
     * @return
     */
    public Collection<String> loadSpeciesNames(Integer speciesId) {
        final Map<String, Set<String>> all = queryProcessor.selectTwoColumns("official_name", "species_name",
                "items.species_names",
                TwoColumnRowMapper.<String, String>multiValMapper(),
                "species_id = :species_id and species_name not like '% %';",
                new MapSqlParameterSource("species_id", speciesId));
        List<String> names = new ArrayList<>();
        final Iterator<String> iterator = all.keySet().iterator();
        if (!iterator.hasNext()) {
            //some species don't have synonyms so need to get the official one:
            names.add(loadSpeciesName(speciesId));
            return names;
        }
        final String official = iterator.next();
        names.add(official);
        for (String name : all.get(official)) {
            if (Character.isLowerCase(name.charAt(0))) {
                names.add(name);
            }
        }
        return names;
    }

    public Map<Integer, Set<String>> loadUniProtLinkouts() {
        log.info("loading UniProt ids");
        Map<Integer, Set<String>> table =
                queryProcessor.selectTwoColumns("protein_id", "linkout_url", "items.proteins_linkouts",
                        TwoColumnRowMapper.<Integer, String>multiValMapper(),
                        "linkout_type = 'UniProt'; ", null);
        log.info(table.size() + " UniProt records read");
        return table;
    }

}
