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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Properties holder.
 *
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public final class AppProperties {

    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);

    public static final AppProperties properties = new AppProperties();

    public final File indexDir;

    private AppProperties() {
        Properties props = new Properties();
        try {
            final FileInputStream inStream = new FileInputStream("/opt/stringdb/v8.3/psicquic.properties");
            props.load(inStream);
            inStream.close();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("can't load properties: " + e.getMessage());
        }
        if (!props.containsKey("index_dir")) {
            throw new ExceptionInInitializerError("index_dir property missing!");
        }
        final String index_dir = props.getProperty("index_dir");
        logger.info("opening index at: " + index_dir);
        indexDir = new File(index_dir);
    }

}
