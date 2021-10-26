/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newrelic.server.impl;

import com.newrelic.server.api.Report;

public class ServerReport implements Report {

  private Long totalUnique;
  private Long totalDuplicate;
  private Long uniqueCount;
  private Long duplicateCount;

  public ServerReport(Long totalUnique,
                      Long totalDuplicate,
                      Long uniqueCount,
                      Long duplicateCount) {
    this.totalUnique = totalUnique;
    this.totalDuplicate = totalDuplicate;
    this.uniqueCount = uniqueCount;
    this.duplicateCount = duplicateCount;
  }

  public Long getTotalUnique() {
    return totalUnique;
  }

  public void setTotalUnique(Long totalUnique) {
    this.totalUnique = totalUnique;
  }

  public Long getUniqueCount() {
    return uniqueCount;
  }

  public void setUniqueCount(Long uniqueCount) {
    this.uniqueCount = uniqueCount;
  }

  public Long getDuplicateCount() {
    return duplicateCount;
  }

  public void setDuplicateCount(Long duplicateCount) {
    this.duplicateCount = duplicateCount;
  }

  public Long getTotalDuplicate() {
    return totalDuplicate;
  }

  public void setTotalDuplicate(Long totalDuplicate) {
    this.totalDuplicate = totalDuplicate;
  }


  public String print(){
    return String.format("Received %s unique numbers, %s duplicates. Unique total: %s", uniqueCount,
            duplicateCount, totalUnique);
  }

}
