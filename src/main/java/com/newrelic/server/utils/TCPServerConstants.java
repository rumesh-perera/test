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

package com.newrelic.server.utils;

public class TCPServerConstants {

  public static Integer MAX_NINE_DIGIT_INTEGER = 999999999;
  public static Integer MIN_NINE_DIGIT_INTEGER = 000000000;
  public static Integer SERVER_POOL_SIZE = 7;
  public static Integer SERVER_MAX_CLIENT_SIZE = 5;
  public static Integer SERVER_PORT = 4000;
  public static String TERMINATE_SEQUENCE = "terminate";
  public static String SERVER_DIGIT_PATTERN = "^[0-9]{9}$";
  public static String DEFAULT_LOG_FILE_LOCATION = "./numbers.log";
  public static String LOG_FILE_JVM_PARAM = "log.file";
  public static Integer GRACEFUL_TERMINATION_DURATION = 10000;
  public static Integer SERVER_REPORT_DURATION = 10;

}
