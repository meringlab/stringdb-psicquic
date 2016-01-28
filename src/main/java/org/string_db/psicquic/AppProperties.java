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
package org.string_db.psicquic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.string_db.ProteinRepository;
import org.string_db.SpeciesRepository;
import org.string_db.jdbc.AppConfig;
import org.string_db.jdbc.DriverDataSourceConfig;
import org.string_db.jdbc.GenericQueryProcessor;
import org.string_db.jdbc.ProteinRepositoryJdbc;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Properties holder.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public final class AppProperties {

    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);
    public static final String STRINGDB_VERSION = "v10.0";
    public static final String BUILD_NUMBER = "0";
    public static final String CONFIG_DIR = "/opt/stringdb/" + STRINGDB_VERSION + "/";
    public static final String UNIPROT_IDS = CONFIG_DIR + "string.uniprot.ids.txt";
    public static final AppProperties instance = new AppProperties();
    public final String solrUrl;
    final ApplicationContext ctx;

    /**
     * Read all property files and fill in the fields
     *
     * @throws ExceptionInInitializerError
     */
    private AppProperties() throws ExceptionInInitializerError {
        Properties props = new Properties();
        try {
            final FileInputStream inStream = new FileInputStream(CONFIG_DIR + "psicquic.properties");
            props.load(inStream);
            inStream.close();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("can't load properties: " + e.getMessage());
        }
        if (!props.containsKey("solr_url")) {
            throw new ExceptionInInitializerError("solr_url property missing!");
        }
        solrUrl = props.getProperty("solr_url");

        ctx = new AnnotationConfigApplicationContext(AppConfig.class, DriverDataSourceConfig.class);
    }

    public ProteinRepository getProteinRepository() {
        return ctx.getBean(ProteinRepositoryJdbc.class);
    }

    public SpeciesRepository getSpeciesRepository() {
        return ctx.getBean(SpeciesRepository.class);
    }

    public GenericQueryProcessor getGenericQueryProcessor() {
        return ctx.getBean(GenericQueryProcessor.class);
    }

    public JdbcTemplate getJdbcTemplate() {
        return ctx.getBean(JdbcTemplate.class);
    }
}
