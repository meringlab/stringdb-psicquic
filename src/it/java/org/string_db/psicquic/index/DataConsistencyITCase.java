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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.string_db.psicquic.AppProperties;

import java.util.List;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class DataConsistencyITCase {
    static JdbcTemplate db = AppProperties.instance.getJdbcTemplate();

    @Test
    public void test_if_all_collection_ids_are_mapped() throws Exception {
        final List<String> collections = db.queryForList("SELECT DISTINCT (collection_id) FROM evidence.sets", String.class);
        collections.removeAll(SourceDbLookup.collections.keySet());
        if (!collections.isEmpty()) {
            Assert.fail("not all collections are mapped: " + collections);
        }
    }
}
