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

/**
 * @author Milan Simonovic <milan.simonovic@imls.uzh.ch>
 */
public class Pair<X, Y> {
    private X x;
    private Y y;

    public Pair(X m, Y n) {
        this.x = m;
        this.y = n;
    }

    public Pair(Pair<X, Y> pair) {
        this.x = pair.x;
        this.y = pair.y;
    }

    public X getX() {
        return x;
    }

    public Y getY() {
        return y;
    }

    public void setX(X m) {
        this.x = m;
    }

    public void setY(Y n) {
        this.y = n;
    }

    @Override
    public String toString() {
        return "Pair [" + x + ", " + y + "]";
    }

}