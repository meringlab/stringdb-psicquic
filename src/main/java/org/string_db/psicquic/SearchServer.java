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

package org.string_db.psicquic;

import org.hupo.psi.calimocho.model.Row;

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public interface SearchServer {
    /**
     * Add a new interaction to the index; might not be visible for search
     * before {@link #commit(boolean)} is called.
     *
     * @param row
     * @throws RuntimeException
     */
    void add(Row row) throws RuntimeException;

    /**
     * Write all previously added interactions to the index.
     *
     * @param reopenSearcher true to wait to reopen searcher
     * @throws RuntimeException
     */
    void commit(boolean reopenSearcher) throws RuntimeException;

    /**
     * @return number of indexed documents
     */
    Long countIndexedDocuments();

    /**
     * remove all documents from the index
     *
     * @throws RuntimeException
     */
    void deleteAll() throws RuntimeException;
}
