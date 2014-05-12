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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

enum Evidence {
    NEIGHBOURHOOD, FUSION, COOCCURRENCE, COEXPRESSION, EXPERIMENTAL, DATABASE, TEXTMINING, TRANSFERRED
}

abstract class EvidenceMitabExporter {
    abstract String export(Integer idA, Integer idB, String taxon, String linkout, int score);
}

/**
 * Export each evidence channel as a separate record.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
class MitabRecordBuilder {

    private static final Logger log = Logger.getLogger(MitabRecordBuilder.class);
    /**
     * evidence.sets_items table
     */
    final Map<Integer, Set<String>> proteinsSets;
    /**
     * evidence.sets table
     */
    final Map<String, String> setsCollections;

    final Map<Integer, Pair<String, String>> proteins;
    final Map<Integer, Pair<Integer, String>> species;
    final Map<Evidence, EvidenceMitabExporter> exporters = new HashMap<Evidence, EvidenceMitabExporter>();
    final Map<Integer, String> stringUniprotIds;
    private Map<Integer, List<String>> refseqIds;

    final Map<String, String> collections = new HashMap<String, String>();
    final Map<String, String> srcDbsNames = new HashMap<String, String>();

    final PostgresConnector db;

    final PreparedStatement selectAbstractsStatement;
    final PreparedStatement selectSetPubmedrefStatement;

    MitabRecordBuilder(PostgresConnector dbcon, Map<Integer, Pair<String, String>> proteins,
                       Map<Integer, Pair<Integer, String>> species, final Map<Integer, Set<String>> ps,
                       final Map<String, String> sc, final Map<Integer, String> uniprotIds, Map<Integer, List<String>> refseqIds) {
        this.db = dbcon;
        this.proteins = proteins;
        this.species = species;
        this.proteinsSets = ps;
        this.setsCollections = sc;
        this.stringUniprotIds = uniprotIds;
        this.refseqIds = refseqIds;
        /*
           * <pre>
            bind: http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0462
            biocarta: http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1108
            biocyc:   http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1105
            dip:   http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0465
            pdb:   http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0460
            grid:  http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0463
            hprd:  http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0468
            intact:http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0469
            kegg:  http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0470
            mint:  http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0471
            GO_complexes: http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0448
            PID http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A1107
            reactome: http://www.ebi.ac.uk/ontology-lookup/?termId=MI%3A0467
            </pre>
           */
        collections.put("bind", "psi-mi:\"MI:0462\"(bind)");
        collections.put("dip", "psi-mi:\"MI:0465\"(dip)");
        collections.put("pdb", "psi-mi:\"MI:0460\"(pdb)");
        collections.put("grid", "psi-mi:\"MI:0463\"(grid)");
        collections.put("hprd", "psi-mi:\"MI:0468\"(hprd)");
        collections.put("intact", "psi-mi:\"MI:0469\"(intact)");
        collections.put("kegg_pathways", "psi-mi:\"MI:0470\"(kegg_pathways)");
        collections.put("mint", "psi-mi:\"MI:0471\"(mint)");
        collections.put("GO_complexes", "psi-mi:\"MI:0448\"(go_complexes)");
        collections.put("reactome", "psi-mi:\"MI:0467\"(reactome)");

        collections.put("biocarta", "psi-mi:\"MI:1108\"(biocarta)");
        collections.put("biocyc", "psi-mi:\"MI:1105\"(biocyc)");
        collections.put("pid", "psi-mi:\"MI:1107\"(pid)");

        srcDbsNames.put("bind", "bind");
        srcDbsNames.put("biocarta", "biocarta");
        srcDbsNames.put("biocyc", "biocarta");
        srcDbsNames.put("dip", "dip");
        srcDbsNames.put("pdb", "pdb");
        srcDbsNames.put("grid", "grid");
        srcDbsNames.put("hprd", "hprd");
        srcDbsNames.put("intact", "intact");
        srcDbsNames.put("kegg_pathways", "kegg");
        srcDbsNames.put("PID", "pid");
        srcDbsNames.put("mint", "mint");
        srcDbsNames.put("GO_complexes", "go");
        srcDbsNames.put("reactome", "reactome");

        try {
            selectAbstractsStatement = db.getConnection()
                    .prepareStatement(
                            "select distinct(p1.abstract_id) from evidence.items_abstracts p1, evidence.items_abstracts p2 "
                                    + " where p1.protein_id = ? and p2.protein_id= ? "
                                    + " and p1.abstract_id = p2.abstract_id limit 3");
            selectSetPubmedrefStatement = db.getConnection().prepareStatement(
                    "select DISTINCT(pubmed_id)  from evidence.sets_pubmedrefs where set_id = ?");

        } catch (Exception e1) {
            throw new ExceptionInInitializerError(e1);
        }

        exporters.put(Evidence.TEXTMINING, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                StringBuilder sb = new StringBuilder();
                sb.append("psi-mi:\"MI:0087\"(predictive text mining)\t");
                // first author
                sb.append("-\t");
                // pubids
                try {
                    selectAbstractsStatement.setInt(1, idA);
                    selectAbstractsStatement.setInt(2, idB);
                    ResultSet ids = selectAbstractsStatement.executeQuery();
                    boolean have = false;
                    while (ids.next()) {
                        have = true;
                        String id = ids.getString(1);
                        if (id.startsWith("PMID0")) {
                            id = id.substring(5);
                            sb.append("pubmed:");
                        } else if (id.startsWith("OMIM0")) {
                            id = id.substring(5);
                            sb.append("omim:");
                        } else if (id.startsWith("SGD-")) {
                            id = "\"" + id.substring(4) + "\"";
                            sb.append("sgd:");
                        } else if (id.startsWith("TIF")) {
                            id = "\"" + id + "\"";
                            sb.append("sdb:");
                        } else {
                            log.warn("can't figure out pubmedid:" + id);
                            return null;
                        }
                        sb.append(id).append("|");
                    }
                    if (!have) {
//						log.warn("missing pubmed id for: " + idA + " " + idB);
                        return null;
                    }
                    sb.setCharAt(sb.length() - 1, '\t');
                    ids.close();
                } catch (Exception e) {
                    log.error(e);
                    throw new RuntimeException(e);
                }
                sb.append(taxon).append(taxon);
                // interaction types, source databases, interaction
                // identifier(s)
                sb.append("psi-mi:\"MI:0190\"\t").append("psi-mi:\"MI:1014\"(string)\t");
                sb.append(linkout);
                sb.append("score:").append(score).append("\n");
                return sb.toString();
            }
        });

        exporters.put(Evidence.DATABASE, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                return exportImported(idA, idB, taxon, score, "psi-mi:\"MI:0364\"(inferred by curator)\t");
            }
        });
        exporters.put(Evidence.EXPERIMENTAL, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                return exportImported(idA, idB, taxon, score,
                        "psi-mi:\"MI:0045\"(experimental interaction detection)\t");
            }
        });

        exporters.put(Evidence.COOCCURRENCE, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                StringBuilder sb = new StringBuilder();
                sb.append("psi-mi:\"MI:0085\"(phylogenetic profile)\t");
                // first author,pubid - not available
                sb.append("-\t-\t");

                sb.append(taxon).append(taxon);
                sb.append("psi-mi:\"MI:0190\"\t").append("psi-mi:\"MI:1014\"(string)\t");
                sb.append(linkout);
                sb.append("score:").append(score).append("\n");
                return sb.toString();
            }
        });
        exporters.put(Evidence.COEXPRESSION, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                StringBuilder sb = new StringBuilder();
                sb.append("psi-mi:\"MI:0686\"(unspecified method - coexpression)\t");
                // first author,pubid - not available
                sb.append("-\t-\t");

                sb.append(taxon).append(taxon);
                sb.append("psi-mi:\"MI:0190\"\t").append("psi-mi:\"MI:1014\"(string)\t");
                sb.append(linkout);
                sb.append("score:").append(score).append("\n");
                return sb.toString();

            }
        });
        exporters.put(Evidence.FUSION, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                StringBuilder sb = new StringBuilder();
                sb.append("psi-mi:\"MI:0036\"(domain fusion)\t");
                // first author,pubid - not available
                sb.append("-\t-\t");

                sb.append(taxon).append(taxon);
                sb.append("psi-mi:\"MI:0190\"\t").append("psi-mi:\"MI:1014\"(string)\t");
                sb.append(linkout);
                sb.append("score:").append(score).append("\n");
                return sb.toString();
            }
        });
        exporters.put(Evidence.NEIGHBOURHOOD, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                StringBuilder newRow = new StringBuilder();
                // interaction detection method
                newRow.append("psi-mi:\"MI:0057\"(gene neighbourhood)\t");
                // first author,pubid
                newRow.append("-\t-\t");
                // taxon for A and B
                newRow.append(taxon).append(taxon);
                // interaction types, source databases, interaction
                // identifier(s)
                newRow.append("psi-mi:\"MI:0190\"\t").append("psi-mi:\"MI:1014\"(string)\t");
                newRow.append(linkout);
                newRow.append("score:").append(score).append("\n");
                return newRow.toString();
            }
        });
        exporters.put(Evidence.TRANSFERRED, new EvidenceMitabExporter() {
            @Override
            String export(Integer idA, Integer idB, String taxon, String linkout, int score) {
                StringBuilder sb = new StringBuilder();
                sb.append("psi-mi:\"MI:0064\"(interologs mapping)\t");
                // first author,pubid - not available
                sb.append("-\t-\t");

                sb.append(taxon).append(taxon);
                sb.append("psi-mi:\"MI:0190\"\t").append("psi-mi:\"MI:1014\"(string)\t");
                sb.append(linkout);
                sb.append("score:").append(score).append("\n");
                return sb.toString();
            }
        });


    }

    Collection<String> makeMitabRecords(Integer idA, Integer idB, int taxon, int combined,
                                        int score_neighb, int score_neighb_tr, int score_fusion, int score_cooccurrence, int score_coexpresion,
                                        int score_coexpression_tr, int score_experimental, int score_experimental_tr, int score_database,
                                        int score_database_tr, int score_textmining, int score_textmining_tr) {
        if (!this.proteins.containsKey(idA) || !this.proteins.containsKey(idB) || !this.species.containsKey(taxon)) {
            throw new IllegalArgumentException("non existing protein/species: " + idA + " / " + idB + " / " + taxon);
        }
        StringBuilder row = new StringBuilder(512);
        // interactor A
        row.append("string:");
        row.append(this.proteins.get(idA).getX());
        if (stringUniprotIds.containsKey(idA)) {
            row.append("|uniprotkb:").append(stringUniprotIds.get(idA));
        }

        row.append("\t");
        // interactor B
        row.append("string:");
        row.append(this.proteins.get(idB).getX());
        if (stringUniprotIds.containsKey(idB)) {
            row.append("|uniprotkb:").append(stringUniprotIds.get(idB));
        }
        row.append("\t");

        // interactor A, B - alternative identifiers, refseq
        // @see https://docs.google.com/document/pub?id=11HpddNs-Bt5a4KOPGCXWivJ4MROYFbY2nhpk4PkvbTA
        if (refseqIds.containsKey(idA) && !refseqIds.get(idA).isEmpty()) {

            for (Iterator<String> iterator = refseqIds.get(idA).iterator(); iterator.hasNext(); ) {
                row.append("refseq:").append(iterator.next());
                if (iterator.hasNext()) {
                    row.append("|");
                }
            }
        } else {
            row.append("-");
        }
        row.append("\t");

        if (refseqIds.containsKey(idB) && !refseqIds.get(idB).isEmpty()) {
            for (Iterator<String> iterator = refseqIds.get(idB).iterator(); iterator.hasNext(); ) {
                row.append("refseq:").append(iterator.next());
                if (iterator.hasNext()) {
                    row.append("|");
                }
            }
        } else {
            row.append("-");
        }
        row.append("\t");

        List<String> records = new ArrayList<String>();
        // aliases A, aliases B
        // escape names in case they contain search engine keywards:
        row.append("string:\"").append(this.proteins.get(idA).getY()).append("\"\t");
        row.append("string:\"").append(this.proteins.get(idB).getY()).append("\"\t");

        // taxon for A and B is the same
        String spc = "taxid:" + taxon + "(" + this.species.get(taxon).getY() + ")\t";

        String linkout = "string:\"" + this.proteins.get(idA).getX() + "%0D" + this.proteins.get(idB).getX() + "\"\t";


        if (score_neighb > 0) {
            records.add(row.toString() + exporters.get(Evidence.NEIGHBOURHOOD).export(idA, idB, spc, linkout, score_neighb));
        }
        if (score_fusion > 0) {
            records.add(row.toString() + exporters.get(Evidence.FUSION).export(idA, idB, spc, linkout, score_fusion));
        }
        if (score_cooccurrence > 0) {
            records
                    .add(row.toString()
                            + exporters.get(Evidence.COOCCURRENCE).export(idA, idB, spc, linkout, score_cooccurrence));
        }
        if (score_coexpresion > 0) {
            records.add(row.toString() + exporters.get(Evidence.COEXPRESSION).export(idA, idB, spc, linkout, score_coexpresion));
        }
        if (score_experimental > 0) {
            String experimental = exporters.get(Evidence.EXPERIMENTAL).export(idA, idB, spc, linkout, score_experimental);
            if (experimental != null) {
                records.add(row.toString() + experimental);
            }
        }
        if (score_database > 0) {
            String dbexport = exporters.get(Evidence.DATABASE).export(idA, idB, spc, linkout, score_database);
            if (dbexport != null) {
                records.add(row.toString() + dbexport);
            }
        }
        if (score_textmining > 0) {
            String tm = exporters.get(Evidence.TEXTMINING).export(idA, idB, spc, linkout, score_textmining);
            if (tm != null) {
                records.add(row.toString() + tm);
            }
        }
        if (score_neighb_tr > 0 || score_coexpression_tr > 0 || score_experimental_tr > 0 || score_database_tr > 0
                || score_textmining_tr > 0) {
            int score = (int)
                    (1000 *
                            (1.0d -
                                    (1.0d - score_coexpression_tr / 1000.0d)
                                            * (1.0d - score_database_tr / 1000.0d)
                                            * (1.0d - score_experimental_tr / 1000.0d)
                                            * (1.0d - score_textmining_tr / 1000.0d)));
            if (score > 1000) {
                log.error("illegal score for: " + idA + "-" + idB + ", transferred score: " + score);
            } else {
                records.add(row.toString() + exporters.get(Evidence.TRANSFERRED).export(idA, idB, spc, linkout, score));
            }
        }

        return records;
    }


    private String exportImported(Integer idA, Integer idB, String taxon, int score, String miTerm) {

        if (!proteinsSets.containsKey(idA) || !proteinsSets.containsKey(idB)) {
            log.warn("no set for: " + idA + " " + idB + ", miterm: " + miTerm);
            return null;
        }
        // source databases, interaction identifier(s)
        try {
            for (String set : proteinsSets.get(idA)) {
                if (proteinsSets.get(idB).contains(set)) {
                    String collection = setsCollections.get(set);
                    if (collections.containsKey(collection)) {
                        StringBuilder sb = new StringBuilder();
                        //Interaction detection methods, from:
                        //http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI:0001&termName=interaction%20detection%20method
                        sb.append(miTerm);
                        // first author - should be obtained from the source db
                        sb.append("-\t");
                        // pubmed id - from evidence.sets_pubmedrefs
                        selectSetPubmedrefStatement.setString(1, set);
                        ResultSet rs = selectSetPubmedrefStatement.executeQuery();
                        //there can be more than one pubmed, this might not give reproducible results
                        boolean pubmedAdded = false;
                        if (rs.next()) {
                            if (rs.getString(1).startsWith("PMID0")) {
                                sb.append("pubmed:").append(rs.getString(1).substring(5));
                                pubmedAdded = true;
                            }
                            while (rs.next()) {
                                if (rs.getString(1).startsWith("PMID0")) {
                                    if (pubmedAdded) {
                                        sb.append("|");
                                    }
                                    sb.append("pubmed:").append(rs.getString(1).substring(5));
                                    pubmedAdded = true;
                                }
                            }

                        }
                        if (!pubmedAdded) {
                            sb.append("-");
                        }
                        sb.append("\t");

                        // taxonomy
                        sb.append(taxon).append(taxon);
                        // interaction types - should be obtained from the
                        // source db
                        sb.append("-\t");
                        sb.append(collections.get(collection)).append("\t");
                        if (!set.contains(":")) {
                            sb.append(srcDbsNames.get(collection)).append(":");
                        }
                        sb.append(set).append("\t");
                        sb.append("score:").append(score).append("\n");

                        return sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.warn(e);
        }
        if (!miTerm.contains("MI:0364")) {
            log.warn("no evidence for: " + idA + " " + idB + ", miterm: " + miTerm);
        }
        return null;
    }
}
